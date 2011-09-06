/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import java.util.prefs.Preferences;

public class PrefStomper {
  public static void main(String[] args) throws Exception {
    handleNode(Preferences.userRoot());
    handleNode(Preferences.systemRoot());
  }

  private static void handleNode(Preferences prefs) throws Exception {
    for (String element : prefs.childrenNames()) {
      Preferences node = prefs.node(element);
      for (String child : node.childrenNames()) {
        handleNode(node.node(child));
      }
      System.out.println("Removing " + node);
      node.removeNode();
    }
    prefs.flush();
  }
}
