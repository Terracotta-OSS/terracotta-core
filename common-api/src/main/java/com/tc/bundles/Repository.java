/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles;

import java.net.URL;
import java.util.Collection;

public interface Repository {

  // return specific version
  Collection<URL> search(String groupId, String name, String version);

  // return all without respect to version
  Collection<URL> search(String groupId, String symName);

  String describe();

}
