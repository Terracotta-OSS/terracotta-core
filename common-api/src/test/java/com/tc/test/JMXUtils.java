/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class JMXUtils {
  public static JMXConnector getJMXConnector(String host, int port) throws MalformedURLException, IOException {
    JMXServiceURL url = new JMXServiceURL("service:jmx:jmxmp://" + host + ":" + port);
    return JMXConnectorFactory.connect(url);
  }

  /*
   * username and password must not be null. If either is null, they won't be used
   */
  public static JMXConnector getJMXConnector(String username, String password, String host, int port)
      throws MalformedURLException, IOException {
    Map env = new HashMap();
    if (username != null && password != null) {
      String[] creds = { username, password };
      env.put("jmx.remote.credentials", creds);
      String addr = MessageFormat.format("service:jmx:rmi:///jndi/rmi://{0}:{1}/jmxrmi",
                                         new Object[] { host, port + "" });
      return JMXConnectorFactory.connect(new JMXServiceURL(addr), env);
    } else {
      return getJMXConnector(host, port);
    }
  }
}
