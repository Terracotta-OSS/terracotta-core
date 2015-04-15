/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.management.web.utils;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * @author Ludovic Orban
 */
public class TSAConfig {

  private static final int DEFAULT_SECURITY_TIMEOUT = 10000;
  private static final int DEFAULT_L1_BRIDGE_TIMEOUT = 15000;

  public static boolean isSslEnabled() {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      Object secure = mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "Secure");
      return Boolean.TRUE.equals(secure);
    } catch (Exception e) {
      return false;
    }
  }

  public static String getSecurityServiceLocation() {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      Object response = mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "SecurityServiceLocation");
      return (String)response;
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      throw new RuntimeException("Error getting SecurityServiceLocation", e);
    }
  }

  public static Integer getSecurityTimeout() {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      Object response = mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "SecurityServiceTimeout");

      if (response == null) {
        return DEFAULT_SECURITY_TIMEOUT;
      }

      return (Integer)response;
    } catch (Exception e) {
      throw new RuntimeException("Error getting SecurityServiceTimeout", e);
    }
  }

  public static String getManagementUrl() {
    if (!isSslEnabled()) {
      return null;
    }

    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      Object securityHostnameAttribute = mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "SecurityHostname");
      Object managementPortAttribute = mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ManagementPort");

      return "https://" + securityHostnameAttribute.toString() + ":" + managementPortAttribute + "/tc-management-api";
    } catch (Exception e) {
      throw new RuntimeException("Error building ManagementUrl", e);
    }
  }

  public static String getSecurityCallbackUrl() {
    if (!isSslEnabled()) {
      return null;
    }

    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      Object securityHostnameAttribute = mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "SecurityHostname");
      Object tsaListenPortAttribute = mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "ManagementPort");

      return "https://" + securityHostnameAttribute + ":" + tsaListenPortAttribute + "/tc-management-api/assertIdentity";
    } catch (Exception e) {
      throw new RuntimeException("Error building SecurityCallbackUrl", e);
    }
  }

  public static String getIntraL2Username() {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      Object response = mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "IntraL2Username");
      return (String)response;
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      throw new RuntimeException("Error getting IntraL2Username", e);
    }
  }

  public static long getDefaultL1BridgeTimeout() {
    try {
      String timeoutString = System.getProperty("com.terracotta.agent.defaultL1BridgeTimeout", "" + DEFAULT_L1_BRIDGE_TIMEOUT);
      return Long.parseLong(timeoutString);
    } catch (NumberFormatException nfe) {
      return DEFAULT_L1_BRIDGE_TIMEOUT;
    }
  }
}
