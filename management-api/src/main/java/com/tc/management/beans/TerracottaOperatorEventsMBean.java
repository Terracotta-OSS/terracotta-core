/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.TerracottaMBean;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventCallback;

public interface TerracottaOperatorEventsMBean extends TerracottaMBean, TerracottaOperatorEventCallback {
  public static final String TERRACOTTA_OPERATOR_EVENT = "terracotta operator event";
  
  void fireOperatorEvent(TerracottaOperatorEvent tcEvent);
}