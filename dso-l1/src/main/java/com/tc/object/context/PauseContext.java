/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All rights
 * reserved.
 */
package com.tc.object.context;

public class PauseContext {

  private final boolean isPause;

  public PauseContext(boolean isPause) {
    this.isPause = isPause;
  }

  public boolean getIsPause() {
    return this.isPause;
  }

}
