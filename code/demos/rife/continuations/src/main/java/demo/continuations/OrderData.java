package demo.continuations;

import java.util.Date;

public class OrderData {
    enum ShippingMethod {ground, express, air}
    enum CreditCardType {amex, visa, mastercard}
    
    private ShippingMethod  shippingMethod;
    private CreditCardType  creditCardType;
    private String  creditCardNumber;
    private String  creditCardExpiration;
    
    public void             setShippingMethod(ShippingMethod shippingMethod)        { this.shippingMethod = shippingMethod; }
    public ShippingMethod   getShippingMethod()                                     { return shippingMethod; }
    public void             setCreditCardType(CreditCardType creditCardType)        { this.creditCardType = creditCardType; }
    public CreditCardType   getCreditCardType()                                     { return creditCardType; }
    public void             setCreditCardNumber(String creditCardNumber)            { this.creditCardNumber = creditCardNumber; }
    public String           getCreditCardNumber()                                   { return creditCardNumber; }
    public void             setCreditCardExpiration(String creditCardExpiration)    { this.creditCardExpiration = creditCardExpiration; }
    public String           getCreditCardExpiration()                               { return creditCardExpiration; }
}

