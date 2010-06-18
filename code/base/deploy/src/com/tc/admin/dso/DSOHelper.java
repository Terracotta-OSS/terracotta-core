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
  private static final DSOHelper helper = new DSOHelper();
  private Icon                   gcIcon;
  private Icon                   topologyIcon;
  private Icon                   featuresIcon;
  private Icon                   diagnosticsIcon;
  private Icon                   clusteredHeapIcon;
  private Icon                   platformIcon;
  private Icon                   monitoringIcon;
  private Icon                   operatorEventIcon;
  private Icon                   bulletErrorIcon;
  private Icon                   bulletGreenIcon;
  private Icon                   bulletOrangeIcon;
  private Icon                   bulletPinkIcon;
  private Icon                   bulletPurpleIcon;
  private Icon                   bulletRedIcon;
  private Icon                   bulletStarIcon;

  private DSOHelper() {/**/
  }

  public static DSOHelper getHelper() {
    return helper;
  }

  public Icon getGCIcon() {
    if (gcIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "trash.gif");
      if (url != null) {
        gcIcon = new ImageIcon(url);
      }
    }
    return gcIcon;
  }

  public Icon getTopologyIcon() {
    if (topologyIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "chart_organisation.png");
      if (url != null) {
        topologyIcon = new ImageIcon(url);
      }
    }
    return topologyIcon;
  }

  public Icon getDiagnosticsIcon() {
    if (diagnosticsIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "magnifier.png");
      if (url != null) {
        diagnosticsIcon = new ImageIcon(url);
      }
    }
    return diagnosticsIcon;
  }

  public Icon getClusteredHeapIcon() {
    if (clusteredHeapIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "database.png");
      if (url != null) {
        clusteredHeapIcon = new ImageIcon(url);
      }
    }
    return clusteredHeapIcon;
  }

  public Icon getPlatformIcon() {
    if (platformIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "newex_wiz.gif");
      if (url != null) {
        platformIcon = new ImageIcon(url);
      }
    }
    return platformIcon;
  }

  public Icon getMonitoringIcon() {
    if (monitoringIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "monitor_obj.gif");
      if (url != null) {
        monitoringIcon = new ImageIcon(url);
      }
    }
    return monitoringIcon;
  }

  public Icon getFeaturesIcon() {
    if (featuresIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "plugin.png");
      if (url != null) {
        featuresIcon = new ImageIcon(url);
      }
    }
    return featuresIcon;
  }

  public Icon getOperatorEventIcon() {
    if (operatorEventIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "bell.png");
      if (url != null) {
        operatorEventIcon = new ImageIcon(url);
      }
    }
    return operatorEventIcon;
  }

  public Icon getBulletErrorIcon() {
    if (bulletErrorIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "bullet_error.png");
      if (url != null) {
        bulletErrorIcon = new ImageIcon(url);
      }
    }
    return bulletErrorIcon;
  }

  public Icon getBulletGreenIcon() {
    if (bulletGreenIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "bullet_green.png");
      if (url != null) {
        bulletGreenIcon = new ImageIcon(url);
      }
    }
    return bulletGreenIcon;
  }

  public Icon getBulletOrangeIcon() {
    if (bulletOrangeIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "bullet_orange.png");
      if (url != null) {
        bulletOrangeIcon = new ImageIcon(url);
      }
    }
    return bulletOrangeIcon;
  }

  public Icon getBulletPinkIcon() {
    if (bulletPinkIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "bullet_pink.png");
      if (url != null) {
        bulletPinkIcon = new ImageIcon(url);
      }
    }
    return bulletPinkIcon;
  }

  public Icon getBulletPurpleIcon() {
    if (bulletPurpleIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "bullet_purple.png");
      if (url != null) {
        bulletPurpleIcon = new ImageIcon(url);
      }
    }
    return bulletPurpleIcon;
  }

  public Icon getBulletRedIcon() {
    if (bulletRedIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "bullet_red.png");
      if (url != null) {
        bulletRedIcon = new ImageIcon(url);
      }
    }
    return bulletRedIcon;
  }

  public Icon getBulletStarIcon() {
    if (bulletStarIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "bullet_star.png");
      if (url != null) {
        bulletStarIcon = new ImageIcon(url);
      }
    }
    return bulletStarIcon;
  }
}
