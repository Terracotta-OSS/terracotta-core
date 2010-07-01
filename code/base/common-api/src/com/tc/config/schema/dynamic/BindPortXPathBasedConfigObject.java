/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.dynamic;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;
import com.terracottatech.config.BindPort;

public class BindPortXPathBasedConfigObject extends XPathBasedConfigItem implements BindPortConfigItem {
  private BindPort defaultValue;

  public BindPortXPathBasedConfigObject(ConfigContext context, String xpath, BindPort defaultValue) {
    super(context, xpath, defaultValue);
    this.defaultValue = defaultValue;
  }

  protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
    BindPort bindPort = BindPort.Factory.newInstance();
    Integer port = (Integer) super.fetchDataFromXmlObjectByReflection(xmlObject, "getIntValue");
    if (port == null) { return null; }
    
    String bindAddress = (String) super.fetchDataFromXmlObjectByReflection(xmlObject, "getBind");
    if(bindAddress == null){
      bindAddress = this.defaultValue.getBind();
    }
    
    bindPort.setIntValue(port);
    bindPort.setBind(bindAddress);
    return bindPort;
  }

  public String getBindAddress() {
    return ((BindPort) getObject()).getBind();
  }

  public int getBindPort() {
    return ((BindPort) getObject()).getIntValue();
  }

}
