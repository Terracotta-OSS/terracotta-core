/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class BaseHelper {
  protected Icon m_refreshIcon;
  protected Icon m_threadDumpsIcon;
  protected Icon m_runtimeStatsIcon;
  protected Icon m_statsRecorderIcon;
  
  protected static final String ICONS_PATH = "/com/tc/admin/icons/";

  public Icon getRefreshIcon() {
    if(m_refreshIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"refresh.gif");
      m_refreshIcon = new ImageIcon(url);
    }

    return m_refreshIcon;
  }
  
  public Icon getThreadDumpsIcon() {
    if(m_threadDumpsIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"thread_view.gif");
      m_threadDumpsIcon = new ImageIcon(url);
    }

    return m_threadDumpsIcon;
  }
  
  public Icon getRuntimeStatsIcon() {
    if(m_runtimeStatsIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"chart_bar.png");
      m_runtimeStatsIcon = new ImageIcon(url);
    }

    return m_runtimeStatsIcon;
  }
 
  public Icon getStatsRecorderIcon() {
    if(m_statsRecorderIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"camera.png");
      m_statsRecorderIcon = new ImageIcon(url);
    }

    return m_statsRecorderIcon;
  }
  
}
