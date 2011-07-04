/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ModuleHelper {

  public static boolean areSiblings(List<Module> modules) {
    if (modules.isEmpty()) return false;

    Module other = modules.get(0);
    for (Module module : modules) {
      if (!module.symbolicName().equals(other.symbolicName())) return false;
    }
    return true;
  }

  public static Module getLatest(List<Module> modules) {
    if (modules.isEmpty() || !areSiblings(modules)) return null;

    List<Module> list = new ArrayList<Module>();
    list.addAll(modules);

    Collections.sort(list);
    return list.get(list.size() - 1);
  }

}
