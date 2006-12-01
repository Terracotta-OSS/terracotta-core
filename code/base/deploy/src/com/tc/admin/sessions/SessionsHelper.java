/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.sessions;

import com.tc.admin.ConnectionContext;
import com.tc.management.beans.L1MBeanNames;
import com.tc.management.beans.MBeanNames;

import javax.management.ObjectName;

public class SessionsHelper {
  private static SessionsHelper m_helper = new SessionsHelper();
  private String                m_sessionProductPattern;
  private ObjectName            m_sessionProductQuery;
  private String                m_sessionsMonitorPattern;
  private ObjectName            m_sessionsMonitorQuery;
  
  private SessionsHelper() {
    try {
      m_sessionProductPattern  = L1MBeanNames.SESSION_PRODUCT_PUBLIC.getCanonicalName() + ",*";
      m_sessionProductQuery    = new ObjectName(m_sessionProductPattern);
      m_sessionsMonitorPattern = MBeanNames.SESSION_INTERNAL.getCanonicalName() + ",*";
      m_sessionsMonitorQuery   = new ObjectName(m_sessionsMonitorPattern);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  public static SessionsHelper getHelper() {
    return m_helper;
  }

  public boolean isSessionsProductMBean(ObjectName name) {
    return m_sessionProductQuery.apply(name);
  }
  
  public ObjectName[] getSessionsProductMBeans(ConnectionContext cc) {
    try {
      return cc.queryNames(m_sessionProductPattern);
    } catch (Exception e) {/**/}

    return null;
  }

  public boolean isSessionMonitorMBean(ObjectName name) {
    return m_sessionsMonitorQuery.apply(name);
  }

  public ObjectName[] getSessionMonitorMBeans(ConnectionContext cc) {
    try {
      return cc.queryNames(m_sessionsMonitorPattern);
    } catch (Exception e) {/**/}

    return null;
  }
}
