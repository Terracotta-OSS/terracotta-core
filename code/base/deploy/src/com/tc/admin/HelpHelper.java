/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class HelpHelper extends BaseHelper {
  private static HelpHelper m_helper = new HelpHelper();
  private Icon              m_helpIcon;

  public static HelpHelper getHelper() {
    return m_helper;
  }

  public Icon getHelpIcon() {
    if(m_helpIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"help.gif");
      
      if(url != null) {
        m_helpIcon = new ImageIcon(url);
      }
    }

    return m_helpIcon;
  }
}
