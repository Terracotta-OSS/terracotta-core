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
package com.tc.objectserver.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.management.TerracottaManagement;
import com.tc.management.TerracottaManagement.MBeanKeys;
import com.tc.management.beans.L2MBeanNames;
import com.tc.util.StringUtil;
import java.lang.management.ManagementFactory;
import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 *
 */
public class JMXSubsystem {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsHandler.class);
  private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
  
  public JMXSubsystem() {
  }
  
  private ObjectName getObjectName(String id) throws MalformedObjectNameException {
    ObjectName name = null;
// support some shortcuts regardless of registration
    switch (id) {
      case "Server":
        name = L2MBeanNames.TC_SERVER_INFO;
        break;
      case "Dumper":
        name = L2MBeanNames.DUMPER;
        break;
      case "DSO":
        name = L2MBeanNames.DSO;
        break;
      default:
        name = new ObjectName(TerracottaManagement.MBeanDomain.PUBLIC + ":" + MBeanKeys.NAME + "=" + id);
        break;
    }
    return name;
  }
  
  public String get(String target, String attribute) {
    try {
      return processReturnType(server.getAttribute(getObjectName(target), attribute));
    } catch (Throwable t) {
      String error = "Invalid JMX attribute:" + target + "." + attribute + " " + t.getMessage();
      warn(t, error, target);
      return error;
    }
  }
  
  public String set(String target, String attribute, String value) {
    try {
      server.setAttribute(getObjectName(target), new Attribute(attribute, value));
      return "SUCCESS";
    } catch (Throwable t) {
      String error = "Invalid JMX attribute:" + target + "." + attribute + " " + t.getMessage();
      warn(t, error, target);
      return error;
    }
  }
  
  public String call(String target, String cmd, String arg) {
    try {
      return callMBean(getObjectName(target), cmd, arg);
    } catch (Throwable t) {
      String error = "Invalid JMX call:" + cmd + " " + t.getMessage();
      warn(t, error, target);
      return error;
    }
  }

  private void printInfo(ObjectName name) throws IntrospectionException, InstanceNotFoundException, ReflectionException {
    MBeanInfo info = server.getMBeanInfo(name);
    for ( MBeanOperationInfo op : info.getOperations()) {
      String method = op.getName();
      String para = StringUtil.toString(op.getSignature());
      System.out.println(method + " " + para);
    }
    System.out.println("ATTRIBUTES");
    for (MBeanAttributeInfo att : info.getAttributes()) {
      System.out.println(att.getName());
    }
  }
  
  private String processReturnType(Object value) {
    if (value == null) {
      return "";
    } else if (value.getClass().isArray()) {
      return StringUtil.toString((Object[])value, " ", "", "");
    } else {
      return value.toString();
    }
  }
  
  private String callMBean(ObjectName name, String cmd, String arg) throws InstanceNotFoundException, MBeanException, ReflectionException, AttributeNotFoundException {
    Object result = null;
    
    if (cmd.startsWith("get")) {
      result = server.getAttribute(name, cmd.substring(3));
    } else if (cmd.startsWith("is")) {
      result = server.getAttribute(name, cmd.substring(2));
    } else {
      if (arg == null) {
        result = server.invoke(name, cmd, new Object[0], new String[0]);
      } else {
        result = server.invoke(name, cmd, new Object[] {arg}, new String[] {String.class.getName()});
      }
    }

    return (processReturnType(result));
  }

  private void warn(Throwable t, String error, String target) {
    LOGGER.warn(error, t);
    if (t instanceof InstanceNotFoundException) {
      LOGGER.warn("Please check whether {} is configured", target);
    }
  }
}