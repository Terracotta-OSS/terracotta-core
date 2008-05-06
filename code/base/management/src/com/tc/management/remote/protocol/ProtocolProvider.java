/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.remote.protocol;

import java.util.Map;

import com.tc.management.remote.protocol.terracotta.ServerProvider;

public final class ProtocolProvider {

  private static final String JMX_DEFAULT_CLASSLOADER_PROP  = "jmx.remote.default.class.loader";
  private static final String JMX_PROVIDER_CLASSLOADER_PROP = "jmx.remote.protocol.provider.class.loader";
  private static final String JMX_PROVIDER_PROP             = "jmx.remote.protocol.provider.pkgs";

  public static void addTerracottaJmxProvider(final Map environment) {
    environment.put(JMX_DEFAULT_CLASSLOADER_PROP, ProtocolProvider.class.getClassLoader());
    environment.put(JMX_PROVIDER_CLASSLOADER_PROP, ServerProvider.class.getClassLoader());
    environment.put(JMX_PROVIDER_PROP, ProtocolProvider.class.getPackage().getName());
  }

}
