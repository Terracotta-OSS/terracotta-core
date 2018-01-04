/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.terracotta.connection.client;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;


public class TerracottaClientConfigParams {
  private final List<InetSocketAddress> stripeMembers;
  private boolean disableReconnect = false;
  private ClassLoader clasLoader;
  private final Properties properties = new Properties();

  public TerracottaClientConfigParams() {
    this.stripeMembers = new ArrayList<>();
  }

  public List<InetSocketAddress> getStripeMemberUris() {
    return Collections.unmodifiableList(this.stripeMembers);
  }

  public TerracottaClientConfigParams addStripeMember(InetSocketAddress stripeMember) {
    this.stripeMembers.add(stripeMember);
    return this;
  }
  
  public TerracottaClientConfigParams disableReconnect() {
    disableReconnect = true;
    return this;
  }

  public boolean isDisableReconnect() {
    return disableReconnect;
  }

  public ClassLoader getClassLoader() {
    return clasLoader;
  }

  public TerracottaClientConfigParams classLoader(ClassLoader loader) {
    setClassLoader(loader);
    return this;
  }

  public void setClassLoader(ClassLoader loader) {
    this.clasLoader = loader;
  }
  
  public void addGenericProperties(Properties props) {
    this.properties.putAll(props);
  }
  
  public Properties getGenericProperties() {
    return this.properties;
  }

}
