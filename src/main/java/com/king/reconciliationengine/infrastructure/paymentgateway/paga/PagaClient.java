package com.king.reconciliationengine.infrastructure.paymentgateway.paga;

import com.king.reconciliationengine.infrastructure.paymentgateway.paga.dto.PagaCheckoutRequest;
import com.king.reconciliationengine.infrastructure.paymentgateway.paga.dto.PagaVerifyRequest;
import com.king.reconciliationengine.infrastructure.paymentgateway.paga.dto.PagaVerifyResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class PagaClient {

    private final RestClient restClient;
    private final String baseUrl;

    public PagaClient(@Value("${paga.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public String buildCheckoutLink(PagaCheckoutRequest payload) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl + "/checkout/params")
                .queryParam("public_key", payload.publicKey())
                .queryParam("amount", payload.amount())
                .queryParam("email", payload.email());

        if (payload.currency() != null) builder.queryParam("currency", payload.currency());
        if (payload.paymentReference() != null) builder.queryParam("payment_reference", payload.paymentReference());
        if (payload.chargeUrl() != null) builder.queryParam("charge_url", payload.chargeUrl());
        if (payload.phoneNumber() != null) builder.queryParam("phone_number", payload.phoneNumber());
        if (payload.displayImage() != null) builder.queryParam("display_image", payload.displayImage());
        if (payload.callbackUrl() != null) builder.queryParam("callback_url", payload.callbackUrl());
        if (payload.fundingSources() != null) builder.queryParam("funding_sources", payload.fundingSources());

        return builder.toUriString();
    }

    public PagaVerifyResponse verify(PagaVerifyRequest payload, String authHeader) {
        return restClient.post()
                .uri("/checkout/transaction/verify")
                .header("Authorization", authHeader)
                .body(payload)
                .retrieve()
                .toEntity(PagaVerifyResponse.class)
                .getBody();
    }
}