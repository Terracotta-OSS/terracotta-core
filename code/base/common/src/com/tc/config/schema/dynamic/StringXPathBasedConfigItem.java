/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;

/**
 * An {@link XPathBasedConfigItem} that returns a {@link String}.
 */
public class StringXPathBasedConfigItem extends XPathBasedConfigItem implements StringConfigItem {

  public StringXPathBasedConfigItem(ConfigContext context, String xpath) {
    super(context, xpath);
  }

  protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
    return super.fetchDataFromXmlObjectByReflection(xmlObject, "getStringValue");
  }

  protected Object fetchDataFromDefaultValue(String defaultValue) {
    return defaultValue;
  }

  public String getString() {
    return (String) getObject();
  }

}
