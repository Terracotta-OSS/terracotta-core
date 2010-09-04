/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.XPathBasedConfigItem;
import com.terracottatech.config.Offheap;

public class OffHeapConfigItem extends XPathBasedConfigItem {

  private final ConfigContext context;

  public OffHeapConfigItem(ConfigContext context, String xpath) {
    super(context, xpath);
    this.context = context;
  }

  @Override
  protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
    if (xmlObject == null) {
      return new OffHeapConfigObject(context.booleanItem("dso/persistence/offheap/enabled").getBoolean(), context
          .stringItem("dso/persistence/offheap/maxDataSize").getString());
    } else {
      return new OffHeapConfigObject(((Offheap) xmlObject).getEnabled(), ((Offheap) xmlObject).getMaxDataSize());
    }
  }

  public OffHeapConfigObject getOffHeapConfigObject() {
    return (OffHeapConfigObject) getObject();
  }

}
