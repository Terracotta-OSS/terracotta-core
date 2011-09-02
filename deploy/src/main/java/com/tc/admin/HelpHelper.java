/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class HelpHelper extends BaseHelper {
  private static HelpHelper helper = new HelpHelper();
  private Icon              helpIcon;

  public static HelpHelper getHelper() {
    return helper;
  }

  public Icon getHelpIcon() {
    if (helpIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "help.gif");
      if (url != null) {
        helpIcon = new ImageIcon(url);
      }
    }
    return helpIcon;
  }
}
