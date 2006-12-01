/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.change;

import com.tc.object.change.event.ArrayElementChangeEvent;
import com.tc.object.change.event.LogicalChangeEvent;
import com.tc.object.change.event.PhysicalChangeEvent;


public interface TCChangeBufferEventVisitor {

  void visitLogicalEvent(LogicalChangeEvent event);

  void visitPhysicalChangeEvent(PhysicalChangeEvent event);

  void visitArrayElementChangeEvent(ArrayElementChangeEvent event);

}

