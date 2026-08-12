package com.king.reconciliationengine.infrastructure.paymentgateway.paga;

import com.king.reconciliationengine.domain.payment.dto.CheckoutDto;
import com.king.reconciliationengine.infrastructure.paymentgateway.paga.dto.PagaCheckoutRequest;
import com.king.reconciliationengine.infrastructure.paymentgateway.paga.dto.PagaCheckoutResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

public class PagaClient {
    private final RestClient restClient;

    public PagaClient(
    ) {
        restClient = RestClient.builder().baseUrl("https://checkout.paga.com").build();
    }

    public PagaCheckoutResponse checkout(PagaCheckoutRequest payload){
        return restClient.post()
                .uri("/checkout")
                .body(payload)
                .retrieve()
                .toEntity(PagaCheckoutResponse.class)
                .getBody();
    }
}
