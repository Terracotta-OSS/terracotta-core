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
package com.tc.management.remote.protocol;

import java.util.Map;

import com.tc.management.remote.protocol.terracotta.ServerProvider;

public final class ProtocolProvider {

  private static final String JMX_DEFAULT_CLASSLOADER_PROP  = "jmx.remote.default.class.loader";
  private static final String JMX_PROVIDER_CLASSLOADER_PROP = "jmx.remote.protocol.provider.class.loader";
  private static final String JMX_PROVIDER_PROP             = "jmx.remote.protocol.provider.pkgs";

  public static void addTerracottaJmxProvider(final Map environment) {
    environment.put(JMX_DEFAULT_CLASSLOADER_PROP, new MultiplexingClassLoader());
    environment.put(JMX_PROVIDER_CLASSLOADER_PROP, new MultiplexingClassLoader());
    environment.put(JMX_PROVIDER_PROP, ProtocolProvider.class.getPackage().getName());
  }

  private static final class MultiplexingClassLoader extends ClassLoader {

    private final ClassLoader classLoader1 = ProtocolProvider.class.getClassLoader();
    private final ClassLoader classLoader2 = ServerProvider.class.getClassLoader();

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
      try {
        return classLoader1.loadClass(name);
      } catch (ClassNotFoundException e) {
        try {
          return classLoader2.loadClass(name);
        } catch (ClassNotFoundException e1) {
          return Thread.currentThread().getContextClassLoader().loadClass(name);
        }
      }
    }
  }

}
