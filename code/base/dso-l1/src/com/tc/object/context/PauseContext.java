/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.context;

import com.tc.async.api.EventContext;

public class PauseContext implements EventContext {

  public static final PauseContext PAUSE   = new PauseContext(true);
  public static final PauseContext UNPAUSE = new PauseContext(false);

  private final boolean            isPause;

  private PauseContext(boolean isPause) {
    this.isPause = isPause;
  }

  public boolean getIsPause() {
    return isPause;
  }

}
