/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import org.dijon.PopupMenu;

import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XTreeNode;

import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

public class WebAppNode extends XTreeNode {
  private WebApp         m_webApp;
  private WebAppLinkNode m_tomcat1Link;
  private WebAppLinkNode m_tomcat2Link;
  private PopupMenu      m_popupMenu;
  private RefreshAction  m_refreshAction;
  private RemoveAction   m_removeAction;
  
  private static ImageIcon ICON;
  static {
    String uri = "/com/tc/admin/icons/package_obj.gif";
    URL    url = WebAppNode.class.getResource(uri);
    
    if(url != null) {
      ICON = new ImageIcon(url);
    }
  }
  
  public WebAppNode(WebApp webApp) {
    super(webApp.getName());

    setWebApp(webApp);
    setIcon(ICON);
    
    add(m_tomcat1Link = new WebAppLinkNode("http://localhost:9081/"+m_webApp));
    add(m_tomcat2Link = new WebAppLinkNode("http://localhost:9082/"+m_webApp));

    initPopup();
  }
  
  private void initPopup() {
    String path = m_webApp.getPath();
    
    if(path != null && path.length() > 0) {
      m_popupMenu = new PopupMenu();
      m_popupMenu.add(m_refreshAction = new RefreshAction());
      m_popupMenu.add(m_removeAction = new RemoveAction());
    }
  }
  
  public JPopupMenu getPopupMenu() {
    return m_popupMenu;
  }

  void setRefreshEnabled(boolean enabled) {
    if(m_refreshAction != null) {
      m_refreshAction.setEnabled(enabled);
    }
  }
  
  class RefreshAction extends XAbstractAction {
    RefreshAction() {
      super("Refresh");
      String uri = "/com/tc/admin/icons/refresh.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }
    
    public void actionPerformed(ActionEvent ae) {
      ((WebAppTreeModel)getModel()).refresh(getWebApp());
    }
  }
  
  void setRemoveEnabled(boolean enabled) {
    if(m_removeAction != null) {
      m_removeAction.setEnabled(enabled);
    }
  }
  
  class RemoveAction extends XAbstractAction {
    RemoveAction() {
      super("Remove");
      String uri = "/com/tc/admin/icons/terminate_co.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }
    
    public void actionPerformed(ActionEvent ae) {
      ((WebAppTreeModel)getModel()).remove(getWebApp());
    }
  }
  
  public void setWebApp(WebApp webApp) {
    m_webApp = webApp;
  }

  public WebApp getWebApp() {
    return m_webApp;
  }
  
  public void updateLinks(boolean tomcat1Ready, boolean tomcat2Ready) {
    m_tomcat1Link.setReady(tomcat1Ready);
    m_tomcat2Link.setReady(tomcat2Ready);
  }
  
  public String getName() {
    return m_webApp.getName();
  }
}
