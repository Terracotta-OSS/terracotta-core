/*
 * Copyright 2001-2007 Geert Bevin <gbevin[remove] at uwyn dot com>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 * $Id$
 */
import com.uwyn.rife.site.ConstrainedBean;
import com.uwyn.rife.site.ConstrainedProperty;
import com.uwyn.rife.site.MetaData;

public class OrderDataMetaData extends MetaData<ConstrainedBean, ConstrainedProperty> {
    public final static String  GROUP_SHIPPING = "shipping";
    public final static String  GROUP_CREDITCARD = "creditcard";
    
    public void activateMetaData() {
        addGroup(GROUP_SHIPPING)
            .addConstraint(new ConstrainedProperty("shippingMethod")
                           .notNull(true));
        
        addGroup(GROUP_CREDITCARD)
            .addConstraint(new ConstrainedProperty("creditCardType")
                           .notNull(true))
            .addConstraint(new ConstrainedProperty("creditCardNumber")
                           .notNull(true)
                           .minLength(16)
                           .maxLength(16)
                           .regexp("\\d+"))
            .addConstraint(new ConstrainedProperty("creditCardExpiration")
                           .notNull(true)
                           .maxLength(5)
                           .regexp("\\d{2}/\\d{2}"));
    }
}
