/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;


public interface TerracottaOperatorEventCallback {
  void logOperatorEvent(TerracottaOperatorEvent event);
}
