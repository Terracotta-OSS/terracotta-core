/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.UIManager;

public class LAFHelper {
  public static String getDefaultLAF() {
    String name = System.getProperty("os.name");
    if (name.indexOf("Windows") > -1) { return "com.sun.java.swing.plaf.windows.WindowsLookAndFeel"; }
    return null;
  }

  public static String[] parseLAFArgs(String[] args) {
    ArrayList al = new ArrayList(Arrays.asList(args));
    String lafType = getDefaultLAF();
    int index = al.indexOf("-laf");

    if (index > -1) {
      al.remove(index);

      if (al.size() - 1 >= index) {
        String laf = (String) al.get(index);

        al.remove(index);

        if ("motif".equals(laf)) {
          lafType = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
        } else if ("metal".equals(laf)) {
          lafType = "javax.swing.plaf.metal.MetalLookAndFeel";
        } else if ("mac".equals(laf)) {
          lafType = "apple.laf.AquaLookAndFeel";
        } else if ("windows".equals(laf)) {
          lafType = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        }
      }
    }

    if (lafType != null) {
      try {
        UIManager.setLookAndFeel(lafType);
        System.setProperty("swing.defaultlaf", lafType);
      } catch (Exception ignore) {/**/
      }
    }

    return (String[]) al.toArray(new String[0]);
  }

}
