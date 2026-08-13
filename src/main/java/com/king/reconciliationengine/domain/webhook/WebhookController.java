package com.king.reconciliationengine.domain.webhook;

import com.king.reconciliationengine.common.response.Response;
import com.king.reconciliationengine.domain.webhook.dto.WebhookPayload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/paga")
    public ResponseEntity<Response<Void>> handlePagaWebhook(@Valid @RequestBody WebhookPayload payload) {
        return webhookService.processCall(payload);
    }
}