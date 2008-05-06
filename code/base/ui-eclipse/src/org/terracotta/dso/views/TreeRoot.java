/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

import com.terracottatech.config.TcConfigDocument.TcConfig;

public class TreeRoot {
  public static final Object EMPTY_ROOT = new Object();
  private TcConfig fConfig;

  public TreeRoot(TcConfig config) {
    this.fConfig = config;
  }

  TcConfig getRoot() {
    return fConfig;
  }
}
