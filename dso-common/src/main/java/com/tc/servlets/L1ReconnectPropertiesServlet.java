/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.servlets;

import org.apache.commons.io.IOUtils;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.properties.TCPropertiesConsts;
import com.terracottatech.config.L1ReconnectPropertiesDocument;
import com.terracottatech.config.L1ReconnectPropertiesDocument.L1ReconnectProperties;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class L1ReconnectPropertiesServlet extends HttpServlet {
  public static final String                  GATHER_L1_RECONNECT_PROP_FROM_L2 = L1ReconnectPropertiesServlet.class
                                                                                   .getName()
                                                                                 + ".l1reconnectpropfroml2";
  private L1ReconnectPropertiesDocument l1ReconnectPropertiesDoc         = null;

  @Override
  public void init() {
    if (l1ReconnectPropertiesDoc == null) {
      l1ReconnectPropertiesDoc = L1ReconnectPropertiesDocument.Factory.newInstance();
      TCProperties l2Properties = TCPropertiesImpl.getProperties();
      L1ReconnectProperties l1ReconnectProperties = l1ReconnectPropertiesDoc.addNewL1ReconnectProperties();
      l1ReconnectProperties.setL1ReconnectEnabled(l2Properties.getBoolean(TCPropertiesConsts.L2_L1RECONNECT_ENABLED));
      l1ReconnectProperties.setL1ReconnectTimeout(new BigInteger(l2Properties
          .getProperty(TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS)));
      l1ReconnectProperties.setL1ReconnectSendqueuecap(new BigInteger(l2Properties
          .getProperty(TCPropertiesConsts.L2_L1RECONNECT_SENDQUEUE_CAP)));
      l1ReconnectProperties.setL1ReconnectMaxDelayedAcks(new BigInteger(l2Properties
          .getProperty(TCPropertiesConsts.L2_L1RECONNECT_MAX_DELAYEDACKS)));
      l1ReconnectProperties.setL1ReconnectSendwindow(new BigInteger(l2Properties
          .getProperty(TCPropertiesConsts.L2_L1RECONNECT_SEND_WINDOW)));
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    OutputStream out = response.getOutputStream();
    IOUtils.copy(this.l1ReconnectPropertiesDoc.newInputStream(), out);

    response.flushBuffer();
  }
}
