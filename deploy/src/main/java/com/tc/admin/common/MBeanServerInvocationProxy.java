/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.NotificationEmitter;
import javax.management.ObjectName;

/*
 * This should be used to get any MBean proxies
 */

public class MBeanServerInvocationProxy extends MBeanServerInvocationHandler {

  public MBeanServerInvocationProxy(MBeanServerConnection connection, ObjectName objectName) {
    super(connection, objectName);
  }

  public static <T> T newMBeanProxy(MBeanServerConnection connection, ObjectName objectName, Class<T> interfaceClass,
                                    boolean notificationBroadcaster) {
    final InvocationHandler handler = new MBeanServerInvocationProxy(connection, objectName);
    final Class[] interfaces;
    if (notificationBroadcaster) {
      interfaces = new Class[] { interfaceClass, NotificationEmitter.class };
    } else {
      interfaces = new Class[] { interfaceClass };
    }
    Object proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(), interfaces, handler);
    return interfaceClass.cast(proxy);
  }

}
