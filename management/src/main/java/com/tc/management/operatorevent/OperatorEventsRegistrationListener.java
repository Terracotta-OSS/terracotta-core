/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.operatorevent;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.MBeanNames;

import javax.management.MBeanServer;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

public class OperatorEventsRegistrationListener implements NotificationListener {
  private static final TCLogger      LOGGER = TCLogging.getLogger(OperatorEventsRegistrationListener.class);
  private final OperatorEventsLogger l1OperatorEventsLogger;
  private final MBeanServer          l2MbeanServer;

  public OperatorEventsRegistrationListener(MBeanServer beanServer) {
    this.l2MbeanServer = beanServer;
    this.l1OperatorEventsLogger = new OperatorEventsLogger();
  }

  @Override
  public void handleNotification(Notification notif, Object data) {
    String type = notif.getType();
    if (notif instanceof MBeanServerNotification) {
      final MBeanServerNotification mbsn = (MBeanServerNotification) notif;
      final ObjectName on = mbsn.getMBeanName();

      if (type.equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
        ObjectName pattern;
        try {
          pattern = new ObjectName(MBeanNames.OPERATOR_EVENTS_PUBLIC.getCanonicalName() + ",*");
        } catch (Exception e) {
          LOGGER.error("Unable to remove listener from MBeanServerDelegate", e);
          return;
        }
        if (pattern.apply(on)) {
          if (this.l2MbeanServer.isRegistered(on)) {
            try {
              this.l2MbeanServer.removeNotificationListener(on, this.l1OperatorEventsLogger);
            } catch (Exception e) {
              LOGGER.error("Unable to remove listener from MBeanServerDelegate", e);
            }
          }
        }
      } else if (type.equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
        ObjectName pattern;
        try {
          pattern = new ObjectName(MBeanNames.OPERATOR_EVENTS_PUBLIC.getCanonicalName() + ",*");
        } catch (Exception e) {
          LOGGER.error("Unable to add listener from MBeanServerDelegate", e);
          return;
        }
        if (pattern.apply(on)) {
          try {
            this.l2MbeanServer.addNotificationListener(on, this.l1OperatorEventsLogger, new OperatorEventsFilter(),
                                                       null);
          } catch (Exception e) {
            LOGGER.error("Unable to add listener from MBeanServerDelegate", e);
          }
        }
      }
    }
  }

}
