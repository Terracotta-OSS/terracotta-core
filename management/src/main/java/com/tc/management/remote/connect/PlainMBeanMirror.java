/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.connect;

import java.io.IOException;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class PlainMBeanMirror implements MBeanMirror {
  private final MBeanServerConnection mbsc;
  private final ObjectName            objectName;
  private final ObjectName            localObjectName;
  private final MBeanInfo             mbeanInfo;

  public PlainMBeanMirror(MBeanServerConnection mbsc, ObjectName objectName, ObjectName localObjectName)
      throws IOException, InstanceNotFoundException, IntrospectionException {
    this.mbsc = mbsc;
    this.objectName = objectName;
    this.localObjectName = localObjectName;
    try {
      this.mbeanInfo = mbsc.getMBeanInfo(objectName);
    } catch (ReflectionException e) {
      // Callers cannot possibly care about the difference between
      // IntrospectionException and ReflectionException
      IntrospectionException ie = new IntrospectionException(e.getMessage());
      ie.initCause(e);
      throw ie;
    }
  }

  public MBeanServerConnection getMBeanServerConnection() {
    return mbsc;
  }

  public ObjectName getRemoteObjectName() {
    return objectName;
  }

  public ObjectName getLocalObjectName() {
    return localObjectName;
  }

  public Object getAttribute(String name) throws AttributeNotFoundException, MBeanException, ReflectionException {
    try {
      return mbsc.getAttribute(objectName, name);
    } catch (IOException e) {
      throw new MBeanException(e);
    } catch (InstanceNotFoundException e) {
      throw new MBeanException(e);
    }
  }

  public void setAttribute(Attribute attr) throws AttributeNotFoundException, InvalidAttributeValueException,
      MBeanException, ReflectionException {
    try {
      mbsc.setAttribute(objectName, attr);
    } catch (IOException e) {
      throw new MBeanException(e);
    } catch (InstanceNotFoundException e) {
      throw new MBeanException(e);
    }
  }

  public AttributeList getAttributes(String[] names) {
    try {
      return mbsc.getAttributes(objectName, names);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      return new AttributeList();
    }
  }

  public AttributeList setAttributes(AttributeList attrs) {
    try {
      return mbsc.setAttributes(objectName, attrs);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      return new AttributeList();
    }
  }

  public Object invoke(String opName, Object[] args, String[] sig) throws MBeanException, ReflectionException {
    try {
      return mbsc.invoke(objectName, opName, args, sig);
    } catch (IOException e) {
      throw new MBeanException(e);
    } catch (InstanceNotFoundException e) {
      throw new MBeanException(e);
    }
  }

  public MBeanInfo getMBeanInfo() {
    return mbeanInfo;
  }
}
