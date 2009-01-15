/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.util.prefs.Preferences;

public class PrefsHelper {
  private static final PrefsHelper helper = new PrefsHelper();

  public static PrefsHelper getHelper() {
    return helper;
  }

  public Preferences userNodeFor(Object object) {
    return userNodeForClass(object.getClass());
  }

  public Preferences userNodeForClass(Class clas) {
    String path = "/" + clas.getName().replace('.', '/');
    return Preferences.userRoot().node(path);
  }

  public String[] keys(Preferences prefs) {
    try {
      return prefs.keys();
    } catch (Exception e) {
      return new String[] {};
    }
  }

  public String[] childrenNames(Preferences prefs) {
    try {
      return prefs.childrenNames();
    } catch (Exception e) {
      return new String[] {};
    }
  }

  public void flush(Preferences prefs) {
    try {
      prefs.flush();
    } catch (Exception e) {/**/
    }
  }

  public void clearKeys(Preferences prefs) {
    try {
      prefs.clear();
    } catch (Exception e) {/**/
    }
  }

  public void clearChildren(Preferences prefs) {
    try {
      for (String name : prefs.childrenNames()) {
        prefs.node(name).removeNode();
      }
    } catch (Exception e) {/**/
    }
  }
}
