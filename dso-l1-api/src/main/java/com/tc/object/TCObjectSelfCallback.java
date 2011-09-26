/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

/**
 * Rename this class
 */
public interface TCObjectSelfCallback {

  /**
   * Called when the tcObjectSelf has been removed from the TCObjectSelfStore
   */
  void removedTCObjectSelfFromStore(TCObjectSelf tcoObjectSelf);

  void initializeTCClazzIfRequired(TCObjectSelf tcoObjectSelf);
}
