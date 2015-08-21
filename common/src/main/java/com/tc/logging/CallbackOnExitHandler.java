/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

public interface CallbackOnExitHandler {
  void callbackOnExit(CallbackOnExitState state);
}
