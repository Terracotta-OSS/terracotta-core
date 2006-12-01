/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import com.tc.admin.AdminClientPanel;

import java.util.prefs.Preferences;

public class SessionIntegratorAdminPanel extends AdminClientPanel {
  public SessionIntegratorAdminPanel() {
    super();
  }
  
  protected boolean shouldAddAboutItem() {
    return false;
  }

  protected Preferences getPreferences() {
    SessionIntegratorContext cntx = SessionIntegrator.getContext();
    return cntx.prefs.node("AdminPanel");
  }

  protected void storePreferences() {
    SessionIntegratorContext cntx = SessionIntegrator.getContext();
    cntx.client.storePrefs();
  }
}
