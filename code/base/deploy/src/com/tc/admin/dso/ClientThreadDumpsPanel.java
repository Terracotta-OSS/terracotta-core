/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AbstractThreadDumpsPanel;
import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.management.beans.l1.L1InfoMBean;

import java.util.prefs.Preferences;

public class ClientThreadDumpsPanel extends AbstractThreadDumpsPanel {
  private ClientThreadDumpsNode m_clientThreadDumpsNode;

  public ClientThreadDumpsPanel(ClientThreadDumpsNode clientThreadDumpsNode) {
    super();
    m_clientThreadDumpsNode = clientThreadDumpsNode;
  }

  protected String getThreadDumpText() throws Exception {
    long requestMillis = System.currentTimeMillis();
    L1InfoMBean l1InfoBean = m_clientThreadDumpsNode.getL1InfoBean();
    return l1InfoBean != null ? l1InfoBean.takeThreadDump(requestMillis) : "L1InfoMBean not registered yet";
  }

  protected Preferences getPreferences() {
    AdminClientContext acc = AdminClient.getContext();
    return acc.prefs.node("ClientThreadDumpsPanel");
  }

  public void tearDown() {
    super.tearDown();
    m_clientThreadDumpsNode = null;
  }
}
