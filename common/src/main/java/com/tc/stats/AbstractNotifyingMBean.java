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
 */
package com.tc.stats;

import com.tc.management.AbstractTerracottaMBean;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

public abstract class AbstractNotifyingMBean extends AbstractTerracottaMBean {

  private static final MBeanNotificationInfo[] NOTIFICATION_INFO;
  static {
    final String[] types = new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE };
    final String noticeType = AttributeChangeNotification.class.getName();
    final String description = "An attribute of this MBean has changed";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(types, noticeType, description) };

  }

  private long                                 nextSequenceNumber = 1;

  protected AbstractNotifyingMBean(Class<?> mBeanInterface) throws NotCompliantMBeanException {
    super(mBeanInterface, true);
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return NOTIFICATION_INFO;
  }

  protected synchronized void sendNotification(String type, Object source) {
    sendNotification(new Notification(type, source, nextSequenceNumber++));
  }

  protected synchronized void sendNotification(String msg, String attr, String type,
                                               Object oldVal, Object newVal) {
    sendNotification(new AttributeChangeNotification(this, nextSequenceNumber++, System.currentTimeMillis(), msg, attr,
                                                     type, oldVal, newVal));
  }
  
  protected synchronized void sendNotification(String type, Object source, String message) {
    sendNotification(new Notification(type, source, nextSequenceNumber++, message));
  }
  
  protected synchronized void sendNotification(String type, Object source, Object userData) {
    Notification notification = new Notification(type, source, nextSequenceNumber++);
    notification.setUserData(userData);
    sendNotification(notification);
  }

}
