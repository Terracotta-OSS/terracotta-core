/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.welcome;

import java.util.ListResourceBundle;
import java.util.ResourceBundle;

public class HyperlinkFrameBundle extends ListResourceBundle {
  public HyperlinkFrameBundle() {
    super();
    setParent(ResourceBundle.getBundle("com.tc.admin.common.CommonBundle"));
  }

  public Object[][] getContents() {
    return new Object[][] { { "file.menu.title", "File" }, { "help.menu.title", "Help" },
        { "page.load.error", "Unable to load page." }, { "about.title.prefix", "About " },
        { "quit.action.name", "Exit" } };
  }
}
