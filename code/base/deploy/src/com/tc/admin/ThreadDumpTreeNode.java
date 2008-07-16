/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XTreeNode;

import java.awt.Point;

public abstract class ThreadDumpTreeNode extends XTreeNode {
  private Point m_viewPosition;

  ThreadDumpTreeNode(Object userObject) {
    super(userObject);
  }

  void setViewPosition(Point viewPosition) {
    m_viewPosition = viewPosition;
  }

  Point getViewPosition() {
    return m_viewPosition;
  }

  abstract String getContent();
}
