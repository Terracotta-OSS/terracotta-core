/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.servlets;

import org.apache.commons.io.IOUtils;

import com.tc.l1propertiesfroml2.L1ReconnectConfigImpl;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.terracottatech.config.L1ReconnectPropertiesFromL2Document;
import com.terracottatech.config.L1ReconnectPropertiesFromL2Document.L1ReconnectPropertiesFromL2;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class L1PropertiesFromL2Servlet extends HttpServlet {
  public static final String                  GATHER_L1_RECONNECT_PROP_FROM_L2 = L1PropertiesFromL2Servlet.class.getName() + ".l1reconnectpropfroml2";
  private L1ReconnectPropertiesFromL2Document l1ReconnectPropFromL2Doc = null;

  public void init() {
    if(l1ReconnectPropFromL2Doc == null){
      l1ReconnectPropFromL2Doc = L1ReconnectPropertiesFromL2Document.Factory.newInstance();
      TCProperties l2Properties = TCPropertiesImpl.getProperties();
      L1ReconnectPropertiesFromL2 l1ReconnectPropFromL2 = l1ReconnectPropFromL2Doc.addNewL1ReconnectPropertiesFromL2();
      l1ReconnectPropFromL2.setL1ReconnectEnabled(l2Properties.getBoolean(L1ReconnectConfigImpl.L2_L1RECONNECT_ENABLED));
      l1ReconnectPropFromL2.setL1ReconnectTimeout( new BigInteger(l2Properties.getProperty(L1ReconnectConfigImpl.L2_L1RECONNECT_TIMEOUT)));
    }
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    OutputStream out = response.getOutputStream();
    IOUtils.copy(this.l1ReconnectPropFromL2Doc.newInputStream(), out);

    response.flushBuffer();
  }
}
