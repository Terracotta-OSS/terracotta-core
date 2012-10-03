package com.tc.admin.common;

import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.relation.MBeanServerNotificationFilter;

public class MBeanRegistrationFilter extends MBeanServerNotificationFilter implements java.io.Serializable {
  static final long        serialVersionUID = 42L;

  private final ObjectName pattern;

  public MBeanRegistrationFilter(String pattern) throws MalformedObjectNameException {
    this(new ObjectName(pattern));
  }

  public MBeanRegistrationFilter(ObjectName pattern) {
    this.pattern = pattern;
  }

  @Override
  public boolean isNotificationEnabled(Notification notif) {
    if (notif instanceof MBeanServerNotification) {
      MBeanServerNotification mbsn = (MBeanServerNotification) notif;
      return pattern.apply(mbsn.getMBeanName());
    }
    return false;
  }
}
