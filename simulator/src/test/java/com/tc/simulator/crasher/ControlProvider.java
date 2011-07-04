/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.crasher;

import com.tc.simulator.control.Control;

public interface ControlProvider {
  public Control getOrCreateControlByName(String name, int parties);
}
