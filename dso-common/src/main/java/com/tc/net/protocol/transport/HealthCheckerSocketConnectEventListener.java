/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.event.TCConnectionEvent;

public interface HealthCheckerSocketConnectEventListener {

  public void notifySocketConnectSuccess(TCConnectionEvent successEvent);

  public void notifySocketConnectFail(TCConnectionEvent failureEvent);
}
