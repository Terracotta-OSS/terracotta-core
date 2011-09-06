/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.BaseHelper;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class ClientsHelper extends BaseHelper {
  private static final ClientsHelper helper = new ClientsHelper();
  private Icon                       clientsIcon;
  private Icon                       clientIcon;

  private ClientsHelper() {/**/
  }

  public static ClientsHelper getHelper() {
    return helper;
  }

  public Icon getClientsIcon() {
    if (clientsIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "hierarchicalLayout.gif");
      clientsIcon = new ImageIcon(url);
    }
    return clientsIcon;
  }

  public Icon getClientIcon() {
    if (clientIcon == null) {
      URL url = getClass().getResource(ICONS_PATH + "genericvariable_obj.gif");
      clientIcon = new ImageIcon(url);
    }
    return clientIcon;
  }
}
