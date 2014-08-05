/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import java.util.Set;

public interface ClientHandle {

  void activateTunnelledMBeanDomains(Set<String> tunnelledMBeanDomains);

  void shutdown();

  boolean isOnline();

  Object getPlatformService();

}
