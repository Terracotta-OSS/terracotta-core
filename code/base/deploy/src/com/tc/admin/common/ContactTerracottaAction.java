/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.event.ActionEvent;

public class ContactTerracottaAction extends XAbstractAction {
  private String m_url;
  
  public ContactTerracottaAction(String label, String url) {
    super(label);
    m_url = url;
  }
  
  public void actionPerformed(ActionEvent e) {
    BrowserLauncher.openURL(m_url);
  }
}

