package com.king.reconciliationengine.infrastructure.paymentgateway.paga;

import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
public class PagaService {
    private final RestTemplate restTemplate;



}
