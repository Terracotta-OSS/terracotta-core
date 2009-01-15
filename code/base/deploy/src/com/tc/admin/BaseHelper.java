/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class BaseHelper {
  protected Icon                refreshIcon;
  protected Icon                threadDumpsIcon;
  protected Icon                runtimeStatsIcon;
  protected Icon                statsRecorderIcon;
  protected Icon                statsRecordingIcon;

  protected static final String ICONS_PATH = "/com/tc/admin/icons/";

  public Icon getRefreshIcon() {
    if (refreshIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "refresh.gif");
      refreshIcon = new ImageIcon(url);
    }
    return refreshIcon;
  }

  public Icon getThreadDumpsIcon() {
    if (threadDumpsIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "thread_view.gif");
      threadDumpsIcon = new ImageIcon(url);
    }
    return threadDumpsIcon;
  }

  public Icon getRuntimeStatsIcon() {
    if (runtimeStatsIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "chart_bar.png");
      runtimeStatsIcon = new ImageIcon(url);
    }
    return runtimeStatsIcon;
  }

  public Icon getStatsRecorderIcon() {
    if (statsRecorderIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "camera.png");
      statsRecorderIcon = new ImageIcon(url);
    }
    return statsRecorderIcon;
  }
}
