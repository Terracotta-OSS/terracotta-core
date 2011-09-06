/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.config.schema.L2Info;

import java.io.IOException;

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
import javax.management.remote.JMXConnector;

public class ConnectionContext {
  public static final String   HOST_PREF_KEY         = "host";
  public static final String   PORT_PREF_KEY         = "port";
  public static final String   AUTO_CONNECT_PREF_KEY = "auto-connect";

  public static final String   DEFAULT_HOST          = "localhost";
  public static final int      DEFAULT_PORT          = 9520;
  public static final boolean  DEFAULT_AUTO_CONNECT  = false;

  public static final int      DSO_SMALL_BATCH_SIZE  = 10;
  public static final int      DSO_MEDIUM_BATCH_SIZE = 100;
  public static final int      DSO_LARGE_BATCH_SIZE  = 500;
  public static final int      DSO_MAX_BATCH_SIZE    = -1;

  public String                host;
  public int                   port;
  public JMXConnector          jmxc;
  public MBeanServerConnection mbsc;
  public MBeanHelper           mbeanHelper;

  public ConnectionContext() {
    this.mbeanHelper = MBeanHelper.getHelper();
  }

  public ConnectionContext(String host, int port) {
    this();

    this.host = host;
    this.port = port;
  }

  public ConnectionContext(L2Info l2Info) {
    this();

    this.host = l2Info.host();
    this.port = l2Info.jmxPort();
  }

  public synchronized void reset() {
    jmxc = null;
    mbsc = null;
  }

  public synchronized boolean isConnected() {
    return mbsc != null;
  }

  private static final ObjectName MBEAN_SERVER_DELEGATE;
  static {
    try {
      MBEAN_SERVER_DELEGATE = new ObjectName("JMImplementation:type=MBeanServerDelegate");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void testConnection() throws Exception {
    isRegistered(MBEAN_SERVER_DELEGATE);
  }

  public ObjectInstance queryMBean(String query) throws MalformedObjectNameException, IOException {
    return mbsc != null ? mbeanHelper.queryMBean(mbsc, query) : null;
  }

  public ObjectName queryName(String query) throws MalformedObjectNameException, IOException {
    return mbsc != null ? mbeanHelper.queryName(mbsc, query) : null;
  }

  public ObjectName[] queryNames(String query) throws MalformedObjectNameException, IOException {
    return mbsc != null ? mbeanHelper.queryNames(mbsc, query) : null;
  }

  public Object getAttribute(ObjectName bean, String attrName) throws MBeanException, AttributeNotFoundException,
      InstanceNotFoundException, ReflectionException, IOException {
    return mbsc != null ? mbeanHelper.getAttribute(mbsc, bean, attrName) : null;
  }

  public void setAttribute(ObjectName bean, String attrName, Object value) throws MBeanException,
      AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException,
      InvalidAttributeValueException {
    if (mbsc != null) {
      mbeanHelper.setAttribute(mbsc, bean, attrName, value);
    }
  }

  public String getStringAttribute(ObjectName bean, String attrName) throws MBeanException, AttributeNotFoundException,
      InstanceNotFoundException, ReflectionException, IOException {
    return mbsc != null ? mbeanHelper.getStringAttribute(mbsc, bean, attrName) : null;
  }

  public boolean getBooleanAttribute(ObjectName bean, String attrName) throws MBeanException,
      AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
    return mbsc != null ? mbeanHelper.getBooleanAttribute(mbsc, bean, attrName) : false;
  }

  public long getLongAttribute(ObjectName bean, String attrName) throws MBeanException, AttributeNotFoundException,
      InstanceNotFoundException, ReflectionException, IOException {
    return mbsc != null ? mbeanHelper.getLongAttribute(mbsc, bean, attrName) : 0L;
  }

  public Object invoke(ObjectName bean, String operation, Object[] args, String[] argTypes)
      throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
    return mbsc != null ? mbeanHelper.invoke(mbsc, bean, operation, args, argTypes) : null;
  }

  public void addNotificationListener(ObjectName bean, NotificationListener listener) throws InstanceNotFoundException,
      IOException {
    if (mbsc != null) {
      safeRemoveNotificationListener(bean, listener);
      mbeanHelper.addNotificationListener(mbsc, bean, listener);
    }
  }

  private void safeRemoveNotificationListener(ObjectName bean, NotificationListener listener) {
    try {
      removeNotificationListener(bean, listener);
    } catch (Exception e) {
      /**/
    }
  }

  public void removeNotificationListener(ObjectName bean, NotificationListener listener)
      throws InstanceNotFoundException, ListenerNotFoundException, IOException {
    if (mbsc != null) {
      mbeanHelper.removeNotificationListener(mbsc, bean, listener);
    }
  }

  public boolean isRegistered(ObjectName bean) throws IOException {
    return mbsc != null ? mbeanHelper.isRegistered(mbsc, bean) : false;
  }

  @Override
  public String toString() {
    return this.host + ":" + this.port;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(port).append(host).toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ConnectionContext) {
      ConnectionContext other = (ConnectionContext) obj;
      return (other.host == this.host || other.host != null && other.host.equals(this.host)) && other.port == this.port;
    }

    return false;
  }
}
