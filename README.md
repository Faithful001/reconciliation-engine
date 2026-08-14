# Reconciliation Engine

A payment processing backend built around a single core problem: your system and your payment gateway's system will eventually disagree about what happened, and something has to catch that before it becomes a customer complaint or a compliance issue.

This project integrates with Paga's Checkout Link product, handles idempotent request processing, verifies inbound webhooks, and runs a scheduled reconciliation job that reconciles internal state against Paga's own record of truth.

## Why this project exists

Most payment integration tutorials stop at "call the provider, get a response, save it." That is not what real payment systems look like. In practice:

- Webhooks get dropped, delayed, or delivered twice
- Network timeouts leave you unsure whether a request actually reached the provider
- Concurrent duplicate requests need to be handled safely, not just sequential retries
- Your internal state can silently drift from the provider's state with no error ever being thrown

This project is built around solving those problems directly, not around the happy path.

## Architecture overview

### The core insight: Paga Checkout Link has no server initiated charge call

Early in this project, the assumption was that the backend would call a "create checkout" endpoint on Paga and receive a synchronous success or failure response, similar to a typical payment gateway. That assumption was wrong, and correcting it shaped the rest of the design.

Paga's Checkout Link product works like this instead:

1. Your backend builds a URL with the payment details encoded as query parameters. No network call is made to build this URL, it is pure string construction.
2. The customer's browser is redirected to that URL, where Paga hosts the actual payment page.
3. After payment, the customer's browser is redirected back to your `charge_url` with query parameters indicating the outcome.
4. Separately, Paga sends a webhook to your `callback_url` with the same outcome, signed with a hash for verification.
5. A `/checkout/transaction/verify` endpoint exists for you to independently confirm a payment's status at any time.

The practical consequence: your backend's `checkout()` endpoint does not know whether a payment succeeded when it responds. It only knows that a checkout attempt was initiated. The webhook, not the initial request, is the source of truth for the outcome.

### Trust boundaries

This project treats three different signals about payment outcome very differently:

| Signal                                  | Trustworthy             | Why                                                                                               |
| --------------------------------------- | ----------------------- | ------------------------------------------------------------------------------------------------- |
| Browser redirect to `charge_url`        | No                      | Plain query parameters, no signature, can be spoofed by anyone who knows or guesses the URL shape |
| Webhook to `callback_url`               | Yes, after verification | Signed with a SHA-512 hash computed from transaction details and a shared secret                  |
| `/checkout/transaction/verify` response | Yes                     | Authenticated, server to server call directly to Paga                                             |

The redirect is used for user experience only, showing the customer a success or failure page. It never triggers a database write. Only the webhook (after hash verification) and the verify endpoint are allowed to change a payment's recorded status.

### Idempotency

Every checkout request requires an `Idempotency-Key` header. The design goals:

- A request that is retried with the same key and the same payload should produce the same outcome as the original request, not a new side effect.
- Two concurrent requests with the same key must not both succeed in creating duplicate records. This is enforced with a database level unique constraint on the key value, not just an application level check, because an application level check alone cannot close the race window between two simultaneous requests.
- A key reused with a different payload is rejected, since replaying a cached response for a different request would silently return the wrong answer to the client.

The idempotency record itself only tracks whether a request is still in flight or has been resolved. It does not duplicate the payment's outcome status. That status lives in one place, on the payment record itself, and the idempotency record simply defers to it once resolved. Early versions of this project stored the outcome in both places, which created two sources of truth that could disagree if one write succeeded and the other did not. That duplication was removed once identified.

### Webhook verification

Every inbound webhook is verified before any of its data is trusted. The signature is computed as a SHA-512 hash over the amount, timestamp, and payment reference, concatenated with a shared secret, and compared against the hash Paga sends. A webhook that fails this check is rejected outright, regardless of what it claims happened.

### Reconciliation

A scheduled job runs on an interval and looks for payments that have been sitting in a pending state longer than a defined threshold, meaning a checkout link was generated but no webhook ever arrived to confirm the outcome. For each of these, the job calls Paga's verify endpoint directly and takes one of three actions:

- If Paga confirms a definitive success or failure, the local record is updated to match. This is treated as safe to do automatically, since the verify endpoint is an authoritative source, not a guess.
- If Paga reports the transaction is still genuinely pending, the record is left alone and checked again on the next run, up to a configurable maximum number of attempts (default: 5). After exhausting those attempts, the payment is escalated to `NEEDS_REVIEW` rather than being retried indefinitely.
- If the response is anything unexpected or ambiguous, the record is flagged for manual review rather than silently resolved either way. The same retry cap applies here.

This job is what catches the case where a webhook was dropped entirely and nothing else in the system would otherwise notice.

In a multi-instance deployment, the scheduled job is protected by a distributed lock using ShedLock, ensuring only one instance executes the reconciliation per interval. The lock is backed by a `shedlock` table in the same PostgreSQL database.

## Request flow

```
1. Client sends POST /payments/checkout with an Idempotency-Key header
2. Server checks for an existing idempotency record
   - If found and still pending, returns the previously generated checkout link
   - If found and resolved, returns the payment's current outcome
   - If not found, proceeds to step 3
3. Server creates a Payment record (status: PENDING) and an IdempotencyKey record
4. Server builds a Paga checkout link and returns it to the client
5. Client redirects the customer to that link
6. Customer completes payment on Paga's hosted page
7. Customer is redirected back to charge_url (UI feedback only, not trusted)
8. Paga sends a signed webhook to the server's callback endpoint
9. Server verifies the webhook signature, then updates the Payment record
10. If no webhook arrives within the reconciliation threshold, the scheduled
    job calls Paga's verify endpoint directly and resolves the record
```

## API endpoints

### `POST /payments/checkout`

Initiates a payment. Requires an `Idempotency-Key` header and an authenticated user.

Request body:

```json
{
  "amount": 1000.0,
  "currency": "NGN",
  "email": "customer@example.com"
}
```

Response:

```json
{
  "success": true,
  "message": "Checkout successful",
  "data": "https://checkout.paga.com/checkout/params?..."
}
```

### `GET /payments/{reference}/status`

Returns the current, verified status of a payment. Scoped to the authenticated user, a payment reference belonging to a different user will return a 404 rather than a 403, to avoid revealing that a payment with that reference exists at all.

### `POST /webhooks/paga`

Receives and processes Paga's payment callbacks. Not authenticated with a JWT, since Paga is the caller, not a logged-in user. Signature verification on the payload is the actual trust boundary for this endpoint.

## Data model

### `payments`

The current state of a single payment attempt. One row per checkout, status transitions from `PENDING` to either `CAPTURED` or `FAILED`. Includes a `reconciliation_attempts` counter tracking how many times the reconciliation job has checked this payment.

### `idempotency_keys`

Tracks whether a specific client request has already been handled. Related one to one with a payment. Holds the checkout link while a request is pending, and defers to the payment's own status once resolved. Enforces a unique constraint on the key value at the database level to prevent concurrent duplicate processing.

### `payment_status_history`

An append-only audit trail recording every payment status transition. Each row captures the previous status (`from_status`, nullable for initial creation), the new status (`to_status`), the source of the change (`CHECKOUT`, `WEBHOOK`, or `RECONCILIATION`), and the timestamp of the transition. This provides a complete, queryable timeline of how any payment arrived at its current state.

### `users`

Standard user account information, referenced by payments to support ownership scoped status lookups.

### `shedlock`

Used internally by ShedLock to coordinate distributed scheduling. Not a JPA entity — the table must be created manually using the DDL in `src/main/resources/db/migration/V1__create_shedlock_table.sql`.

## Setup

### Prerequisites

- Java 21 or later
- PostgreSQL
- A Paga business account with API credentials (sandbox is sufficient for development)

### Environment variables

```
POSTGRES_DB_URL=jdbc:postgresql://localhost:5435/your-db-name
POSTGRES_DB=your-db-name
POSTGRES_USER=your-db-user
POSTGRES_PASSWORD=your-db-password

JWT_SECRET=your-jwt-secret

PAGA_BASE_URL=paga-base-url
PAGA_SECRET_KEY=your-paga-secret
PAGA_PUBLIC_KEY=your-paga-public-key

PAGA_AUTH_HEADER=Basic your-paga-token
PAGA_WEBHOOK_SECRET=your-webhook-secret
WEBHOOK_CALLBACK_URL=your-webhook-callback-url

PAYMENT_PAYLOAD_SECRET_KEY=your-payment-payload-secret
```

Use `PAGA_BASE_URL=https://beta-checkout.paga.com` for testing against Paga's sandbox environment, and `https://checkout.paga.com` for production.

`PAGA_AUTH_HEADER` should include the `Basic` prefix as shown, since it is passed directly as the value of the `Authorization` header on the verify request, not just the encoded credential.

`PAYMENT_PAYLOAD_SECRET_KEY` is the key used to compute the internal request hash stored on each idempotency record, used to detect a key being reused with a different payload. This is separate from `PAGA_WEBHOOK_SECRET`, which is Paga's own HMAC key used to verify inbound webhook signatures. The two must not be confused or reused for each other, since they protect different trust boundaries: one is internal request integrity, the other is authenticity of a message actually coming from Paga.

### Running locally

```bash
./mvnw spring-boot:run
```

The reconciliation job requires `@EnableScheduling` to be active, which is configured on the main application class. Confirm the job is actually firing on startup by checking the application logs for its scheduled log output, since a scheduled method on a class Spring is not managing as a bean will fail to register with no error.

**Important:** Before running the application, create the ShedLock table by executing the SQL in `src/main/resources/db/migration/V1__create_shedlock_table.sql` against your PostgreSQL database. This table is not managed by Hibernate's `ddl-auto` since it is not a JPA entity.

## Design principles this project tries to demonstrate

- Idempotency should be enforced at the database level, not only in application logic, since only a database constraint can close a race condition between two truly concurrent requests.
- A webhook, redirect, or any inbound signal should never be trusted to change financial state without independent verification.
- When a background process detects a discrepancy between internal state and an external source of truth, the response should depend on how definitive that external answer is. A confirmed answer can be applied automatically. An ambiguous one should be escalated to a human, never guessed at.
- Duplicated state across two entities that are supposed to always agree is a bug waiting to happen, not a safety net. A single source of truth for any given fact should be established, even if that means simplifying a design after the fact.
