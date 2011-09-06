/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.io.TCRandomFileAccess;
import com.tc.util.startuplock.FileNotCreatedException;
import com.tc.util.startuplock.LocationNotCreatedException;

public interface StartupLock {
  boolean isBlocked();

  boolean canProceed(TCRandomFileAccess randomFileAccess) throws LocationNotCreatedException, FileNotCreatedException;

  void release();
}
