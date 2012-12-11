/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.test.jmx;

import java.io.Serializable;

public interface CustomCommandExecutor {

  public Serializable execute(String cmd, Serializable[] params);
}
