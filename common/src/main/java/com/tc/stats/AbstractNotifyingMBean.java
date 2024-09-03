/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
