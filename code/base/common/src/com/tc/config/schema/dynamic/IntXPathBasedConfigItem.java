/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;
import com.tc.util.Assert;

import java.math.BigInteger;

/**
 * An {@link XPathBasedConfigItem} that extracts a Java <code>int</code>.
 */
public class IntXPathBasedConfigItem extends XPathBasedConfigItem implements IntConfigItem {

  private static final BigInteger MAX_INT_AS_BIG_INTEGER = new BigInteger(new Integer(Integer.MAX_VALUE).toString());
  private static final BigInteger MIN_INT_AS_BIG_INTEGER = new BigInteger(new Integer(Integer.MIN_VALUE).toString());

  public IntXPathBasedConfigItem(ConfigContext context, String xpath) {
    super(context, xpath);

    try {
      if (!context.hasDefaultFor(xpath) && context.isOptional(xpath)) {
        // formatting
        throw Assert
            .failure("XPath '" + xpath + "' is optional and has no default. As such, you can't use it in "
                     + "a ConfigItem returning only an int; what will we return if it's not there? Add a default "
                     + "in the schema, or make it mandatory.");
      }
    } catch (XmlException xmle) {
      throw Assert.failure("Unable to fetch default for '" + xpath + "'.");
    }
  }

  protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
    BigInteger out = (BigInteger) super.fetchDataFromXmlObjectByReflection(xmlObject, "getBigIntegerValue");
    
    if (out == null) return null;
    
    boolean fits = (out.compareTo(MAX_INT_AS_BIG_INTEGER) <= 0) && (out.compareTo(MIN_INT_AS_BIG_INTEGER) >= 0);
    if (!fits) throw Assert.failure("Value " + out
                                    + " is too big to represent as an 'int'; you should either be using a "
                                    + "ConfigItem that uses a BigInteger to represent its data, or the schema should "
                                    + "restrict this value to one that fits in a Java 'int'.");
    return new Integer(out.intValue());
  }

  public int getInt() {
    return ((Integer) getObject()).intValue();
  }

}
