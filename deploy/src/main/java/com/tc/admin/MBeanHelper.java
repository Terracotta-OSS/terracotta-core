/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import java.io.IOException;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class MBeanHelper {
  private static final MBeanHelper helper = new MBeanHelper();

  protected MBeanHelper() {/**/
  }

  public static final MBeanHelper getHelper() {
    return helper;
  }

  public ObjectInstance queryMBean(MBeanServerConnection mbsc, String query) throws IOException,
      MalformedObjectNameException {
    ObjectInstance result = null;

    if (mbsc != null) {
      Set mbeans = mbsc.queryMBeans(new ObjectName(query), null);
      if (mbeans != null && mbeans.size() > 0) {
        result = (ObjectInstance) (mbeans.toArray(new ObjectInstance[] {})[0]);
      }
    }

    return result;
  }

  public ObjectName queryName(MBeanServerConnection mbsc, String query) throws MalformedObjectNameException,
      IOException {
    ObjectName result = null;

    if (mbsc != null) {
      Set names = mbsc.queryNames(new ObjectName(query), null);
      if (names != null && names.size() > 0) {
        result = (ObjectName) names.toArray(new ObjectName[] {})[0];
      }
    }

    return result;
  }

  public ObjectName[] queryNames(MBeanServerConnection mbsc, String query) throws MalformedObjectNameException,
      IOException {
    ObjectName[] result = null;

    if (mbsc != null) {
      Set names = mbsc.queryNames(new ObjectName(query), null);
      if (names != null && names.size() > 0) {
        result = (ObjectName[]) names.toArray(new ObjectName[] {});
      }
    }

    return result;
  }

  public Object getAttribute(MBeanServerConnection mbsc, ObjectName bean, String attr) throws MBeanException,
      AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
    if (bean == null) { throw new IllegalArgumentException("ObjectName is null"); }
    return mbsc != null ? mbsc.getAttribute(bean, attr) : null;
  }

  public long getLongAttribute(MBeanServerConnection mbsc, ObjectName bean, String attr) throws MBeanException,
      AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
    if (bean == null) { throw new IllegalArgumentException("ObjectName is null"); }
    Object obj = getAttribute(mbsc, bean, attr);
    if (obj != null && obj instanceof Long) { return ((Long) obj).longValue(); }
    return 0L;
  }

  public String getStringAttribute(MBeanServerConnection mbsc, ObjectName bean, String attr) throws MBeanException,
      AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
    if (bean == null) { throw new IllegalArgumentException("ObjectName is null"); }
    Object obj = getAttribute(mbsc, bean, attr);
    return (obj != null) ? obj.toString() : null;
  }

  public boolean getBooleanAttribute(MBeanServerConnection mbsc, ObjectName bean, String attr) throws MBeanException,
      AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
    if (bean == null) { throw new IllegalArgumentException("ObjectName is null"); }
    Object obj = getAttribute(mbsc, bean, attr);
    if (obj != null && obj instanceof Boolean) { return ((Boolean) obj).booleanValue(); }
    return false;
  }

  public Object invoke(MBeanServerConnection mbsc, ObjectName bean, String method, Object[] types, String[] args)
      throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
    if (bean == null) { throw new IllegalArgumentException("ObjectName is null"); }
    return mbsc != null ? mbsc.invoke(bean, method, types, args) : null;
  }

  public void setAttribute(MBeanServerConnection mbsc, ObjectName bean, String attr, Object value)
      throws InstanceNotFoundException, MBeanException, ReflectionException, IOException, AttributeNotFoundException,
      InvalidAttributeValueException {
    if (bean == null) { throw new IllegalArgumentException("ObjectName is null"); }
    if (mbsc != null) {
      mbsc.setAttribute(bean, new Attribute(attr, value));
    }
  }

  public void addNotificationListener(MBeanServerConnection mbsc, ObjectName bean, NotificationListener listener)
      throws InstanceNotFoundException, IOException {
    if (bean == null) { throw new IllegalArgumentException("ObjectName is null"); }
    if (mbsc != null) {
      mbsc.addNotificationListener(bean, listener, null, null);
    }
  }

  public void removeNotificationListener(MBeanServerConnection mbsc, ObjectName bean, NotificationListener listener)
      throws InstanceNotFoundException, ListenerNotFoundException, IOException {
    if (bean == null) { throw new IllegalArgumentException("ObjectName is null"); }
    if (mbsc != null) {
      mbsc.removeNotificationListener(bean, listener);
    }
  }

  public boolean isRegistered(MBeanServerConnection mbsc, ObjectName bean) throws IOException {
    return mbsc != null ? mbsc.isRegistered(bean) : false;
  }
}
