/*
 * Copyright 2001-2007 Geert Bevin <gbevin[remove] at uwyn dot com>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 * $Id$
 */
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

