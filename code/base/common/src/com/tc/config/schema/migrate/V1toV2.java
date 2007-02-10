/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.migrate;

import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import com.terracottatech.configV1.Application;
import com.terracottatech.configV1.DsoApplication;
import com.terracottatech.configV1.DsoClientData;
import com.terracottatech.configV1.DsoServerData;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.namespace.QName;

/*
 * Converts from the V1 to the V2 configuration format. The namespace has permanently changed from
 * http://www.terracottatech.com/config.v1 to http://www.terracotta.org/config. DSO and JMX are now required. The
 * following are gone: - JDBC - embedded HTTP server (Jetty) and HTTP interface to JMX -
 * DsoClientData.maxInMemoryObjectCount - DsoServerData.serverCachedObjectCount - ConfigurationModel.DEMO - DSO
 * ChangeListener
 */

public class V1toV2 extends BaseConfigUpdate {
  private static final String V1_NAMESPACE    = "http://www.terracottatech.com/config-v1";
  private static final String V2_NAMESPACE    = "http://www.terracotta.org/config";
  private static final String SCHEMA_LOCATION = "http://www.terracotta.org/schema/terracotta-2.2.xsd";
  private static final String XSI_NAMESPACE   = "http://www.w3.org/2001/XMLSchema-instance";

  protected XmlOptions        defaultXmlOptions;
  private boolean             addSchemaLocation;

  public InputStream convert(InputStream in, XmlOptions xmlOptions) throws XmlException, IOException {
    com.terracottatech.configV1.TcConfigDocument v1Doc = com.terracottatech.configV1.TcConfigDocument.Factory.parse(in);
    if (v1Doc != null) {
      com.terracottatech.configV1.TcConfigDocument.TcConfig v1Config = v1Doc.getTcConfig();

      if (v1Config.isSetSystem()) {
        com.terracottatech.configV1.System system = v1Config.getSystem();
        com.terracottatech.configV1.ConfigurationModel configModel = system.xgetConfigurationModel();

        if (configModel != null) {
          StringEnumAbstractBase value = configModel.enumValue();
          if (value.intValue() == com.terracottatech.configV1.ConfigurationModel.INT_DEMO) {
            configModel.set(com.terracottatech.configV1.ConfigurationModel.DEVELOPMENT);
          }
        }

        if (system.isSetDsoEnabled()) {
          system.unsetDsoEnabled();
        }
        if (system.isSetJdbcEnabled()) {
          system.unsetJdbcEnabled();
        }
        if (system.isSetHttpEnabled()) {
          system.unsetHttpEnabled();
        }
        if (system.isSetJmxEnabled()) {
          system.unsetJmxEnabled();
        }
        if (system.isSetJmxHttpEnabled()) {
          system.unsetJmxHttpEnabled();
        }

        if (!system.isSetConfigurationModel() && !system.isSetLicense()) {
          v1Config.unsetSystem();
        }
      }

      if (v1Config.isSetServers()) {
        com.terracottatech.configV1.Servers servers = v1Config.getServers();
        com.terracottatech.configV1.Server server;
        if (servers != null) {
          for (int i = 0; i < servers.sizeOfServerArray(); i++) {
            server = servers.getServerArray(i);

            if (server.isSetHttpPort()) {
              server.unsetHttpPort();
            }
            if (server.isSetJdbcPort()) {
              server.unsetJdbcPort();
            }
            if (server.isSetJmxHttpPort()) {
              server.unsetJmxHttpPort();
            }

            if (server.isSetDso()) {
              DsoServerData dsoServerData = server.getDso();

              if (dsoServerData.isSetServerCachedObjectCount()) {
                dsoServerData.unsetServerCachedObjectCount();
              }

              if (!dsoServerData.isSetClientReconnectWindow() && !dsoServerData.isSetGarbageCollection()
                  && !dsoServerData.isSetPersistence()) {
                server.unsetDso();
              }
            }
          }
        }
      }

      if (v1Config.isSetClients()) {
        com.terracottatech.configV1.Client client = v1Config.getClients();
        if (client != null) {
          if (client.isSetDso()) {
            DsoClientData dsoClientData = client.getDso();

            if (dsoClientData.isSetMaxInMemoryObjectCount()) {
              dsoClientData.unsetMaxInMemoryObjectCount();
            }

            if (!dsoClientData.isSetDebugging() && !dsoClientData.isSetFaultCount()) {
              client.unsetDso();
            }
          }
        }
      }

      if (v1Config.isSetApplication()) {
        Application application = v1Config.getApplication();

        if (application.isSetJdbc()) {
          application.unsetJdbc();
        }
        if (application.isSetDso()) {
          DsoApplication dsoApp = application.getDso();

          if (dsoApp.isSetChangeListener()) {
            dsoApp.unsetChangeListener();
          }
        }
      }

      if (addSchemaLocation) {
        XmlCursor cursor = v1Doc.newCursor();
        if (cursor.toFirstChild()) {
          QName name = new QName(XSI_NAMESPACE, "schemaLocation");
          cursor.setAttributeText(name, SCHEMA_LOCATION);
        }
        cursor.dispose();
      }

      InputStream inStream = v1Doc.newInputStream(xmlOptions);
      InputStreamReader inReader = new InputStreamReader(inStream);
      BufferedReader bufferedReader = new BufferedReader(inReader);
      StringBuffer sb = new StringBuffer();
      String nl = System.getProperty("line.separator");
      String s;

      try {
        while ((s = bufferedReader.readLine()) != null) {
          sb.append(StringUtils.replace(s, V1_NAMESPACE, V2_NAMESPACE));
          sb.append(nl);
        }
      } catch (IOException ioe) {
        /* this won't happen because the source stream isn't file- or network-based */
      }

      return new ByteArrayInputStream(sb.toString().getBytes());
    }

    return null;
  }
}
