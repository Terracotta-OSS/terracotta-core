/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerProvider;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.generic.GenericConnectorServer;

public class ServerProvider implements JMXConnectorServerProvider {

  public JMXConnectorServer newJMXConnectorServer(final JMXServiceURL jmxServiceURL, final Map initialEnvironment,
                                                  final MBeanServer mBeanServer) throws IOException {
    if (!jmxServiceURL.getProtocol().equals("terracotta")) {
      MalformedURLException exception = new MalformedURLException("Protocol not terracotta: "
                                                                  + jmxServiceURL.getProtocol());
      throw exception;
    }
    Map terracottaEnvironment = initialEnvironment != null ? new HashMap(initialEnvironment) : new HashMap();
    terracottaEnvironment.put(GenericConnectorServer.MESSAGE_CONNECTION_SERVER,
                              new TunnelingMessageConnectionServer(jmxServiceURL));
    return new GenericConnectorServer(terracottaEnvironment, mBeanServer);
  }

}
