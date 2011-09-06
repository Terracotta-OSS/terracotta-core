/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XTreeNode;

import java.awt.Point;

public abstract class ThreadDumpTreeNode extends XTreeNode {
  protected ApplicationContext appContext;
  private Point                viewPosition;

  ThreadDumpTreeNode(ApplicationContext appContext) {
    super();
    this.appContext = appContext;
  }

  ThreadDumpTreeNode(ApplicationContext appContext, Object userObject) {
    super(userObject);
    this.appContext = appContext;
  }

  void setViewPosition(Point viewPosition) {
    this.viewPosition = viewPosition;
  }

  Point getViewPosition() {
    return viewPosition;
  }

  abstract String getContent();

  abstract void cancel();

  abstract boolean isDone();
}
