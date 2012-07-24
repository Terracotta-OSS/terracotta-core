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
import javax.management.remote.rmi.RMIConnectorServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

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

  public static JMXConnector getSecuredJmxConnector(String username, char[] password, String host, int port) {
    SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
    SslRMIServerSocketFactory ssf = new SslRMIServerSocketFactory();
    HashMap<String, Object> env = new HashMap<String, Object>();
    env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, csf);
    env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, ssf);

    // Needed to avoid "non-JRMP server at remote endpoint" error... todo really ?
    env.put("com.sun.jndi.rmi.factory.socket", csf);
    env.put("jmx.remote.credentials", new Object[] {username, password});

    try {
      JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://"+host+":" + port + "/jndi/rmi://" + host + ":" + port + "/jmxrmi");
      return JMXConnectorFactory.connect(url, env);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


}
