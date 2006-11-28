/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema.dynamic;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;

/**
 * An {@link XPathBasedConfigItem} that returns a Java <code>boolean</code> value.
 */
public class BooleanXPathBasedConfigItem extends XPathBasedConfigItem implements BooleanConfigItem {

  public BooleanXPathBasedConfigItem(ConfigContext context, String xpath, boolean defaultValue) {
    super(context, xpath, new Boolean(defaultValue));
  }

  public BooleanXPathBasedConfigItem(ConfigContext context, String xpath) {
    super(context, xpath);
  }

  protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
    return super.fetchDataFromXmlObjectByReflection(xmlObject, "getBooleanValue");
  }

  public boolean getBoolean() {
    return ((Boolean) getObject()).booleanValue();
  }

}
