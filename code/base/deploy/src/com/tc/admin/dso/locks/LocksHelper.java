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
  private static LocksHelper m_helper = new LocksHelper();
  private Icon               m_locksIcon;
  private Icon               m_lockIcon;
  private Icon               m_detectDeadlocksIcon;

  public static LocksHelper getHelper() {
    return m_helper;
  }

  public Icon getLocksIcon() {
    if (m_locksIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "owned_monitor_obj.gif");
      m_locksIcon = new ImageIcon(url);
    }
    return m_locksIcon;
  }

  public Icon getLockIcon() {
    if (m_lockIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "deadlock_view.gif");
      m_lockIcon = new ImageIcon(url);
    }
    return m_lockIcon;
  }

  public Icon getDetectDeadlocksIcon() {
    if (m_detectDeadlocksIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "insp_sbook.gif");
      m_detectDeadlocksIcon = new ImageIcon(url);
    }
    return m_detectDeadlocksIcon;
  }
}
