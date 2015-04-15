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
package com.tc.object.config.schema;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlString;

import com.tc.config.schema.BaseConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.terracottatech.config.Client;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;

public class L1DSOConfigObject extends BaseConfigObject implements L1DSOConfig {

  private static final int FAULT_COUNT = TCPropertiesImpl.getProperties()
                                           .getInt(TCPropertiesConsts.L1_OBJECTMANAGER_FAULT_COUNT);


  public L1DSOConfigObject(ConfigContext context) {
    super(context);
  }

  @Override
  public int faultCount() {
    return FAULT_COUNT;
  }

  public static void initializeClients(TcConfig config, DefaultValueProvider defaultValueProvider) throws XmlException {
    Client client;
    if (!config.isSetClients()) {
      client = config.addNewClients();
    } else {
      client = config.getClients();
    }
    initializeLogsDirectory(client, defaultValueProvider);
  }

  private static void initializeLogsDirectory(Client client, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    Assert.assertNotNull(client);
    if (!client.isSetLogs()) {
      final XmlString defaultValue = (XmlString) defaultValueProvider.defaultFor(client.schemaType(), "logs");
      String substitutedString = ParameterSubstituter.substitute(defaultValue.getStringValue());

      client.setLogs(new File(substitutedString).getAbsolutePath());
    } else {
      Assert.assertNotNull(client.getLogs());
      client.setLogs(ParameterSubstituter.substitute(client.getLogs()));
    }
  }

}
