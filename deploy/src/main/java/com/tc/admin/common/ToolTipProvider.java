/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.event.MouseEvent;

public interface ToolTipProvider {
  String getToolTipText(MouseEvent me);
}
