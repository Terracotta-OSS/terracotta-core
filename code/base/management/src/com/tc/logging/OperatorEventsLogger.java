/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import com.tc.management.beans.TerracottaOperatorEventsMBean;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventCallbackLogger;
import com.tc.util.Assert;

import javax.management.Notification;
import javax.management.NotificationListener;

public class OperatorEventsLogger implements NotificationListener {

  private final TerracottaOperatorEventCallbackLogger callbackLogger = new TerracottaOperatorEventCallbackLogger();

  public void handleNotification(Notification notification, Object handback) {
    Assert.assertTrue(TerracottaOperatorEventsMBean.TERRACOTTA_OPERATOR_EVENT.equals(notification.getType()));

    TerracottaOperatorEvent operatorEvent = (TerracottaOperatorEvent) notification.getUserData();
    callbackLogger.logOperatorEvent(operatorEvent);
  }

}
