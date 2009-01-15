/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.BaseHelper;

import java.net.URL;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * @ThreadUnsafe To be called from the Swing EventDispathThread only.
 */

public class RootsHelper extends BaseHelper {
  private static final RootsHelper helper = new RootsHelper();
  private Icon                     rootsIcon;
  private Icon                     fieldIcon;
  private Icon                     cycleIcon;

  private RootsHelper() {/**/}
  
  public static RootsHelper getHelper() {
    return helper;
  }

  public Icon getRootsIcon() {
    if (rootsIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "hierarchicalLayout.gif");
      if (url != null) {
        rootsIcon = new ImageIcon(url);
      }
    }
    return rootsIcon;
  }

  public Icon getFieldIcon() {
    if (fieldIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "field_protected_obj.gif");
      if (url != null) {
        fieldIcon = new ImageIcon(url);
      }
    }
    return fieldIcon;
  }

  public Icon getCycleIcon() {
    if (cycleIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "obj_cycle.gif");
      if (url != null) {
        cycleIcon = new ImageIcon(url);
      }
    }
    return cycleIcon;
  }

  public String[] trimFields(String[] fields) {
    if (fields != null && fields.length > 0) {
      ArrayList list = new ArrayList();
      for (String field : fields) {
        if (!field.startsWith("this$")) {
          list.add(field);
        }
      }
      return (String[]) list.toArray(new String[0]);
    }
    return new String[] {};
  }
}
