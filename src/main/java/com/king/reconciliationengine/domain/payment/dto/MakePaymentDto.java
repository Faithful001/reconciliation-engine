package com.king.reconciliationengine.domain.payment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record MakePaymentDto(
        @NotBlank
        String public_key,

        @NotNull
        @Positive
        BigDecimal amount,

        String currency,	//String	No	Default is NGN, specify if otherwise
        String payment_reference, //	String	No	Payment identifier, if not provided, paga will generate
        String charge_url,	//String	No	Location to redirect your customer after payment
        String phone_number, //	String	No	Customer's phone number

        @Email
        String email,	// String	Yes	Customer's email address
        String display_image,	// String	No	Merchant preferred image on checkout
        String callback_url,	// String	No	To receive callback for payment, indicate callback url
        String funding_sources	//List	No

) {
}
