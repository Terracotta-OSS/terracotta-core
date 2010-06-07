/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import com.tc.logging.TCLogger;
import com.tc.management.beans.TerracottaOperatorEventsMBean;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventCallbackLogger;
import com.tc.util.Assert;

import javax.management.Notification;
import javax.management.NotificationListener;

public class OperatorEventsLogger implements NotificationListener {

  private final TerracottaOperatorEventCallbackLogger callbackLogger;

  public OperatorEventsLogger(TCLogger logger) {
    this.callbackLogger = new TerracottaOperatorEventCallbackLogger(logger);
  }

  public void handleNotification(Notification notification, Object handback) {
    Assert.assertTrue(TerracottaOperatorEventsMBean.TERRACOTTA_OPERATOR_EVENT.equals(notification.getType()));

    TerracottaOperatorEvent operatorEvent = (TerracottaOperatorEvent) notification.getSource();
    callbackLogger.logOperatorEvent(operatorEvent);
  }

}
