/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.event.ActionEvent;

public class ContactTerracottaAction extends XAbstractAction {
  private String url;

  public ContactTerracottaAction(String label, String url) {
    super(label);
    this.url = url;
  }

  public void actionPerformed(ActionEvent e) {
    BrowserLauncher.openURL(url);
  }
}
