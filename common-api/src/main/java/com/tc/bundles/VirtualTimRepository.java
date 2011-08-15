/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class VirtualTimRepository implements Repository {

  private final Map<String, URL> virtualTimJars;

  public VirtualTimRepository(Map<String, URL> virtualTimJars) {
    this.virtualTimJars = new HashMap<String, URL>(virtualTimJars);
  }

  public Collection<URL> search(String groupId, String name, String version) {
    String key = OSGiToMaven.makeBundleFilename(name, version);
    URL url = virtualTimJars.get(key);
    if (url != null) { return Collections.singletonList(url); }
    return Collections.EMPTY_LIST;
  }

  public Collection<URL> search(String groupId, String symName) {
    Collection<URL> rv = new ArrayList<URL>();
    for (Entry<String, URL> e : virtualTimJars.entrySet()) {
      if (e.getKey().startsWith(symName)) {
        rv.add(e.getValue());
      }
    }
    return rv;
  }

  public String describe() {
    return getClass().getSimpleName() + virtualTimJars;
  }
}