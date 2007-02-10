/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlInteger;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.ObjectArrayConfigItem;
import com.tc.config.schema.dynamic.ObjectArrayXPathBasedConfigItem;
import com.tc.util.Assert;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.System;

/**
 * The standard implementation of {@link L2ConfigForL1}.
 */
public class L2ConfigForL1Object implements L2ConfigForL1 {

  private static final String         DEFAULT_HOST = "localhost";

  private final ConfigContext         l2sContext;
  private final ConfigContext         systemContext;

  private final ObjectArrayConfigItem l2Data;

  private final L2Data                defaultL2Data;

  public L2ConfigForL1Object(ConfigContext l2sContext, ConfigContext systemContext) {
    Assert.assertNotNull(l2sContext);
    Assert.assertNotNull(systemContext);

    this.l2sContext = l2sContext;
    this.systemContext = systemContext;

    this.l2sContext.ensureRepositoryProvides(Servers.class);
    this.systemContext.ensureRepositoryProvides(System.class);

    this.defaultL2Data = new L2Data(DEFAULT_HOST, getL2IntDefault("server/dso-port"));

    this.l2Data = new ObjectArrayXPathBasedConfigItem(this.l2sContext, ".", new L2Data[] { this.defaultL2Data }) {
      protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
        Server[] l2Array = ((Servers) xmlObject).getServerArray();
        L2Data[] data;

        if (l2Array == null || l2Array.length == 0) {
          data = new L2Data[] { defaultL2Data };
        } else {
          data = new L2Data[l2Array.length];

          for (int i = 0; i < data.length; ++i) {
            Server l2 = l2Array[i];
            String host = l2.getHost();
            if (host == null) host = l2.getName();

            if (host == null) host = defaultL2Data.host();
            int dsoPort = l2.getDsoPort() > 0 ? l2.getDsoPort() : defaultL2Data.dsoPort();

            data[i] = new L2Data(host, dsoPort);
          }
        }

        return data;
      }
    };
  }

  private int getL2IntDefault(String xpath) {
    try {
      return ((XmlInteger) l2sContext.defaultFor(xpath)).getBigIntegerValue().intValue();
    } catch (XmlException xmle) {
      throw Assert.failure("Can't fetch default for " + xpath + "?", xmle);
    }
  }

  public ObjectArrayConfigItem l2Data() {
    return this.l2Data;
  }

}
