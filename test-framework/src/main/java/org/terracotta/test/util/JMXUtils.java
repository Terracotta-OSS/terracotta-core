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
package org.terracotta.test.util;

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
