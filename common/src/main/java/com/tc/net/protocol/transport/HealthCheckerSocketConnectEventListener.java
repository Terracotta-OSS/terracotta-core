/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.event.TCConnectionEvent;

public interface HealthCheckerSocketConnectEventListener {

  public void notifySocketConnectSuccess(TCConnectionEvent successEvent);

  public void notifySocketConnectFail(TCConnectionEvent failureEvent);
}
