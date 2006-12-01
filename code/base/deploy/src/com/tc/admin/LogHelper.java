/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class LogHelper extends BaseHelper {
  private static LogHelper m_helper = new LogHelper();
  private Icon             m_alertIcon;
  private Icon             m_warningIcon;
  private Icon             m_errorIcon;
  private Icon             m_infoIcon;
  private Icon             m_blankIcon;

  public static LogHelper getHelper() {
    return m_helper;
  }

  public Icon getAlertIcon() {
    if(m_alertIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"alert12x12.gif");
      
      if(url != null) {
        m_alertIcon = new ImageIcon(url);
      }
    }

    return m_alertIcon;
  }

  public Icon getWarningIcon() {
    if(m_warningIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"warning12x12.gif");
      
      if(url != null) {
        m_warningIcon = new ImageIcon(url);
      }
    }

    return m_warningIcon;
  }

  public Icon getErrorIcon() {
    if(m_errorIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"error12x12.gif");
      
      if(url != null) {
        m_errorIcon = new ImageIcon(url);
      }
    }

    return m_errorIcon;
  }

  public Icon getInfoIcon() {
    if(m_infoIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"info12x12.gif");
      
      if(url != null) {
        m_infoIcon = new ImageIcon(url);
      }
    }

    return m_infoIcon;
  }

  public Icon getBlankIcon() {
    if(m_blankIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"blank12x12.gif");
      
      if(url != null) {
        m_blankIcon = new ImageIcon(url);
      }
    }

    return m_blankIcon;
  }
}
