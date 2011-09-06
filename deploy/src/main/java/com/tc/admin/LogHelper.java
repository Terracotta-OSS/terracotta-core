/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * @ThreadUnsafe To be called from the Swing EventDispathThread only.
 */

public class LogHelper extends BaseHelper {
  private static final LogHelper helper = new LogHelper();
  private Icon                   alertIcon;
  private Icon                   warningIcon;
  private Icon                   errorIcon;
  private Icon                   infoIcon;
  private Icon                   blankIcon;

  public static LogHelper getHelper() {
    return helper;
  }

  public Icon getAlertIcon() {
    if (alertIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "alert12x12.gif");
      if (url != null) {
        alertIcon = new ImageIcon(url);
      }
    }
    return alertIcon;
  }

  public Icon getWarningIcon() {
    if (warningIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "warning12x12.gif");
      if (url != null) {
        warningIcon = new ImageIcon(url);
      }
    }
    return warningIcon;
  }

  public Icon getErrorIcon() {
    if (errorIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "error12x12.gif");
      if (url != null) {
        errorIcon = new ImageIcon(url);
      }
    }
    return errorIcon;
  }

  public Icon getInfoIcon() {
    if (infoIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "info12x12.gif");
      if (url != null) {
        infoIcon = new ImageIcon(url);
      }
    }
    return infoIcon;
  }

  public Icon getBlankIcon() {
    if (blankIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "blank12x12.gif");
      if (url != null) {
        blankIcon = new ImageIcon(url);
      }
    }
    return blankIcon;
  }
}
