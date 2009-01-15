/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XTreeNode;

import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

public class WebAppNode extends XTreeNode {
  private WebApp           webApp;
  private WebAppLinkNode   tomcat1Link;
  private WebAppLinkNode   tomcat2Link;
  private JPopupMenu       popupMenu;
  private RefreshAction    refreshAction;
  private RemoveAction     removeAction;

  private static ImageIcon ICON;
  static {
    String uri = "/com/tc/admin/icons/package_obj.gif";
    URL url = WebAppNode.class.getResource(uri);

    if (url != null) {
      ICON = new ImageIcon(url);
    }
  }

  public WebAppNode(WebApp webApp) {
    super(webApp.getName());

    setWebApp(webApp);
    setIcon(ICON);

    add(tomcat1Link = new WebAppLinkNode("http://localhost:9081/" + webApp));
    add(tomcat2Link = new WebAppLinkNode("http://localhost:9082/" + webApp));

    initPopup();
  }

  private void initPopup() {
    String path = webApp.getPath();
    if (path != null && path.length() > 0) {
      popupMenu = new JPopupMenu();
      popupMenu.add(refreshAction = new RefreshAction());
      popupMenu.add(removeAction = new RemoveAction());
    }
  }

  public JPopupMenu getPopupMenu() {
    return popupMenu;
  }

  void setRefreshEnabled(boolean enabled) {
    if (refreshAction != null) {
      refreshAction.setEnabled(enabled);
    }
  }

  class RefreshAction extends XAbstractAction {
    RefreshAction() {
      super("Refresh");
      String uri = "/com/tc/admin/icons/refresh.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }

    public void actionPerformed(ActionEvent ae) {
      ((WebAppTreeModel) getModel()).refresh(getWebApp());
    }
  }

  void setRemoveEnabled(boolean enabled) {
    if (removeAction != null) {
      removeAction.setEnabled(enabled);
    }
  }

  class RemoveAction extends XAbstractAction {
    RemoveAction() {
      super("Remove");
      String uri = "/com/tc/admin/icons/terminate_co.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }

    public void actionPerformed(ActionEvent ae) {
      ((WebAppTreeModel) getModel()).remove(getWebApp());
    }
  }

  public void setWebApp(WebApp webApp) {
    this.webApp = webApp;
  }

  public WebApp getWebApp() {
    return webApp;
  }

  public void updateLinks(boolean tomcat1Ready, boolean tomcat2Ready) {
    tomcat1Link.setReady(tomcat1Ready);
    tomcat2Link.setReady(tomcat2Ready);
  }

  public String getName() {
    return webApp.getName();
  }
}
