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
  private Icon                   diagnosticsIcon;
  private Icon                   clusteredHeapIcon;

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
  
}
