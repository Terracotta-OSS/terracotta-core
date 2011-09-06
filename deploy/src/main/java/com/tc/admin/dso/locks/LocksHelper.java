/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import com.tc.admin.BaseHelper;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class LocksHelper extends BaseHelper {
  private static final LocksHelper helper = new LocksHelper();
  private Icon                     locksIcon;
  private Icon                     lockIcon;
  private Icon                     detectDeadlocksIcon;

  private LocksHelper() {/**/
  }

  public static LocksHelper getHelper() {
    return helper;
  }

  public Icon getLocksIcon() {
    if (locksIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "owned_monitor_obj.gif");
      locksIcon = new ImageIcon(url);
    }
    return locksIcon;
  }

  public Icon getLockIcon() {
    if (lockIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "deadlock_view.gif");
      lockIcon = new ImageIcon(url);
    }
    return lockIcon;
  }

  public Icon getDetectDeadlocksIcon() {
    if (detectDeadlocksIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "insp_sbook.gif");
      detectDeadlocksIcon = new ImageIcon(url);
    }
    return detectDeadlocksIcon;
  }
}
