/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import java.util.prefs.Preferences;

public class PrefStomper {
  public static void main(String[] args) throws Exception {
    Preferences prefs = Preferences.userRoot();
    String[] children = prefs.childrenNames();

    for (int i = 0; i < children.length; i++) {
      System.out.println("Removing " + children[i]);
      prefs.node(children[i]).removeNode();
    }

    prefs = Preferences.systemRoot();
    children = prefs.childrenNames();
    
    for (int i = 0; i < children.length; i++) {
      System.out.println("Removing " + children[i]);
      prefs.node(children[i]).removeNode();
    }

    prefs.flush();
  }
}
