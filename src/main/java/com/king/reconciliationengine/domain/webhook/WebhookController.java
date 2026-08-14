package com.king.reconciliationengine.domain.webhook;

import com.king.reconciliationengine.common.response.Response;
import com.king.reconciliationengine.domain.webhook.dto.WebhookPayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
@Tag(name = "Webhooks", description = "Incoming payment gateway notification webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @Operation(summary = "Handle Paga payment webhook", description = "Receives and processes payment outcome notifications sent by the Paga payment gateway.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Webhook payload processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or signature hash validation failed")
    })
    @PostMapping("/paga")
    public ResponseEntity<Response<Void>> handlePagaWebhook(@Valid @RequestBody WebhookPayload payload) {
        return webhookService.processCall(payload);
    }
}