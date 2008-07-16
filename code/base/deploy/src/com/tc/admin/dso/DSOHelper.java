/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.BaseHelper;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * @ThreadUnsafe To be called from the Swing EventDispathThread only.
 */

public class DSOHelper extends BaseHelper {
  private static final DSOHelper m_helper = new DSOHelper();
  private Icon                   m_gcIcon;

  public static DSOHelper getHelper() {
    return m_helper;
  }

  public Icon getGCIcon() {
    if (m_gcIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "trash.gif");
      if (url != null) {
        m_gcIcon = new ImageIcon(url);
      }
    }
    return m_gcIcon;
  }
}
