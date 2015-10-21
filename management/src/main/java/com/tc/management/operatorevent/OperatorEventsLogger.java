/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.management.operatorevent;

import com.tc.management.beans.TerracottaOperatorEventsMBean;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.util.Assert;

import javax.management.Notification;
import javax.management.NotificationListener;

public class OperatorEventsLogger implements NotificationListener {

  private final TerracottaOperatorEventLogger         tcEventLogger  = TerracottaOperatorEventLogging.getEventLogger();

  @Override
  public void handleNotification(Notification notification, Object handback) {
    Assert.assertTrue(TerracottaOperatorEventsMBean.TERRACOTTA_OPERATOR_EVENT.equals(notification.getType()));

    TerracottaOperatorEvent operatorEvent = (TerracottaOperatorEvent) notification.getUserData();
    tcEventLogger.fireOperatorEvent(operatorEvent);
  }

}
