/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;

/**
 * An {@link XPathBasedConfigItem} that returns an array of objects. Subclasses must override the
 * {@link #fetchDataFromXmlObject(XmlObject)} method to return the actual array in question.
 */
public abstract class ObjectArrayXPathBasedConfigItem extends XPathBasedConfigItem implements ObjectArrayConfigItem {

  public ObjectArrayXPathBasedConfigItem(ConfigContext context, String xpath) {
    super(context, xpath);
  }

  public ObjectArrayXPathBasedConfigItem(ConfigContext context, String xpath, Object defaultValue) {
    super(context, xpath, defaultValue);
  }

  public Object[] getObjects() {
    return (Object[]) getObject();
  }

}
