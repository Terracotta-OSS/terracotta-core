/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.management.lock.stats.LockStatisticsResponseMessageImpl;

public class LockStatisticsResponseHandler extends AbstractEventHandler {

  public void handleEvent(EventContext context) {
    LockStatisticsResponseMessageImpl msg = (LockStatisticsResponseMessageImpl) context;
    msg.send();
  }

}
