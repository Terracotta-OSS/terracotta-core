/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.simulator.crasher;

import com.tc.simulator.control.Control;

public interface ControlProvider {
  public Control getOrCreateControlByName(String name, int parties);
}
