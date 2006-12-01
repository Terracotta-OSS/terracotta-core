/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

/**
 * Listener to pause/unpause events.  This is currently cused for tests.  --Orion
 */
public interface PauseListener {
  public void notifyPause();
  public void notifyUnpause();
}
