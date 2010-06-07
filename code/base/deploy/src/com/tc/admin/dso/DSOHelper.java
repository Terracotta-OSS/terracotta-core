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
}
