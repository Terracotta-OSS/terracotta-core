/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.sessions;

import org.dijon.ContainerResource;

import com.tc.admin.AdminClient;
import com.tc.admin.common.XContainer;
import com.tc.management.beans.sessions.SessionMonitorMBean;

/**
 * @NotUsed
 */

public class SessionMonitorPanel extends XContainer {
  private SessionMonitorTable m_table;
  private SessionMonitorMBean m_bean;

  public SessionMonitorPanel(SessionMonitorMBean bean) {
    super();
    m_bean = bean;
    load((ContainerResource) AdminClient.getContext().getComponent("SessionMonitorPanel"));
  }

  public void load(ContainerResource containerRes) {
    super.load(containerRes);
    m_table = (SessionMonitorTable) findComponent("SessionMonitorTable");
    m_table.setBean(m_bean);
  }

  public SessionMonitorMBean getBean() {
    return m_bean;
  }

  public void refresh() {
    m_table.refresh();
  }

  public void tearDown() {
    super.tearDown();
  }
}
