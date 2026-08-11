## TODO

public_key	String	Yes	Public Key for your Paga business account
amount	Number	Yes	Amount you want customer to pay
currency	String	No	Default is NGN, specify if otherwise
payment_reference	String	No	Payment identifier, if not provided, paga will generate
charge_url	String	No	Location to redirect your customer after payment
phone_number	String	No	Customer's phone number
email	String	Yes	Customer's email address
display_image	String	No	Merchant preferred image on checkout
callback_url	String	No	To receive callback for payment, indicate callback url
funding_sources	List	No