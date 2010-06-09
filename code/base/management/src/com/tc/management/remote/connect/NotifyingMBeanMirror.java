/*
 * NotifyingMBeanMirror.java , Copyright 2007 Eamonn McManus Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.tc.management.remote.connect;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class NotifyingMBeanMirror implements MBeanMirror, NotificationEmitter {
  private final MBeanMirror mirror;

  public NotifyingMBeanMirror(MBeanMirror mirror) {
    this.mirror = mirror;
  }

  public Object getAttribute(String name) throws AttributeNotFoundException, MBeanException, ReflectionException {
    return mirror.getAttribute(name);
  }

  public void setAttribute(Attribute attr) throws AttributeNotFoundException, InvalidAttributeValueException,
      MBeanException, ReflectionException {
    mirror.setAttribute(attr);
  }

  public AttributeList getAttributes(String[] names) {
    return mirror.getAttributes(names);
  }

  public AttributeList setAttributes(AttributeList attrs) {
    return mirror.setAttributes(attrs);
  }

  public Object invoke(String opName, Object[] args, String[] sig) throws MBeanException, ReflectionException {
    return mirror.invoke(opName, args, sig);
  }

  public MBeanInfo getMBeanInfo() {
    return mirror.getMBeanInfo();
  }

  private class NotificationIntercepter implements NotificationListener {
    private final NotificationListener listener;

    private NotificationIntercepter(NotificationListener listener) {
      this.listener = listener;
    }

    public void handleNotification(Notification notification, Object handback) {
      notification.setSource(mirror.getLocalObjectName());
      listener.handleNotification(notification, handback);
    }
  }

  public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
    try {
      mirror.getMBeanServerConnection()
          .addNotificationListener(getRemoteObjectName(), new NotificationIntercepter(listener), filter, handback);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
    try {
      mirror.getMBeanServerConnection().removeNotificationListener(getRemoteObjectName(), listener);
    } catch (RuntimeException e) {
      throw e;
    } catch (ListenerNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
      throws ListenerNotFoundException {
    try {
      mirror.getMBeanServerConnection().removeNotificationListener(getRemoteObjectName(), listener, filter, handback);
    } catch (RuntimeException e) {
      throw e;
    } catch (ListenerNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public MBeanNotificationInfo[] getNotificationInfo() {
    return mirror.getMBeanInfo().getNotifications();
  }

  public MBeanServerConnection getMBeanServerConnection() {
    return mirror.getMBeanServerConnection();
  }

  public ObjectName getRemoteObjectName() {
    return mirror.getRemoteObjectName();
  }

  public ObjectName getLocalObjectName() {
    return mirror.getLocalObjectName();
  }
}
