/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AbstractThreadDumpsPanel;

import java.util.prefs.Preferences;

public class ClientThreadDumpsPanel extends AbstractThreadDumpsPanel {
  private ClientThreadDumpsNode m_clientThreadDumpsNode;

  public ClientThreadDumpsPanel(ClientThreadDumpsNode clientThreadDumpsNode) {
    super();
    m_clientThreadDumpsNode = clientThreadDumpsNode;
  }

  protected String getThreadDumpText() throws Exception {
    return m_clientThreadDumpsNode.getClient().takeThreadDump(System.currentTimeMillis());
  }

  protected Preferences getPreferences() {
    return m_acc.getPrefs().node("ClientThreadDumpsPanel");
  }

  public void tearDown() {
    super.tearDown();
    m_clientThreadDumpsNode = null;
  }
}
