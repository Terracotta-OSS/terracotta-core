/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.dynamic;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;
import com.terracottatech.config.BindPort;

public class BindPortXPathBasedConfigObject extends XPathBasedConfigItem implements BindPortConfigItem {
  
  private final BindPort bindPort;

  public BindPortXPathBasedConfigObject(ConfigContext context, String xpath, BindPort bindPort) {
    super(context, xpath);
    this.bindPort = bindPort;
  }

  protected Object fetchDataFromXmlObject(XmlObject xmlObject) {

    return bindPort;
  }


  public String getBindAddress() {
    return ((BindPort)getObject()).getBind();
  }

  public int getBindPort() {
    return ((BindPort)getObject()).getIntValue();
  }

}
