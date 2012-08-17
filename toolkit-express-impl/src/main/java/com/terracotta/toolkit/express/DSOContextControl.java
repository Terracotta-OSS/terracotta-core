/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import java.net.URL;
import java.util.Collection;
import java.util.Set;

public interface DSOContextControl {

  void init(Set<String> tunnelledMBeanDomains);

  void shutdown();

  void activateModules(Collection<URL> modules);

}
