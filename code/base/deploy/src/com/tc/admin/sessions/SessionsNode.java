/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.sessions;

import com.tc.admin.AdminClient;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.dso.ClassesHelper;
import com.tc.management.TerracottaManagement;
import com.tc.management.exposed.SessionsProductMBean;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultTreeModel;

public class SessionsNode extends ComponentNode implements NotificationListener {
  private ConnectionContext m_cc;
  private JPopupMenu        m_popupMenu;
  private RefreshAction     m_refreshAction;

  private static final String REFRESH_ACTION = "RefreshAction";
  
  public SessionsNode(ConnectionContext cc, ObjectName[] beanNames) {
    super();

    m_cc = cc;
    
    try {
      ObjectName mbsd = cc.queryName("JMImplementation:type=MBeanServerDelegate");
      
      if(mbsd != null) {
        cc.addNotificationListener(mbsd, this);
      }
    } catch(Exception ioe) {
      ioe.printStackTrace();
    }
    
    for(int i = 0; i < beanNames.length; i++) {
      try {
        addBean(beanNames[i]);
      } catch(IOException ioe) {
        ioe.printStackTrace();
      }
    }
    
    setLabel("Session Statistics");

    initMenu();
  }

  private void addBean(ObjectName name) throws IOException {
    SessionsProductMBean bean = (SessionsProductMBean)
      TerracottaManagement.findMBean(name, SessionsProductMBean.class, m_cc.mbsc);
    SessionsProductNode node = new SessionsProductNode(m_cc, bean, name);  
    DefaultTreeModel model = getModel();
    
    if(model != null) {
      model.insertNodeInto(node, this, getChildCount());
    }
    else {
      add(node);
    }
  }
  
  public void handleNotification(Notification notification, Object handback) {
    if(notification instanceof MBeanServerNotification) {
      MBeanServerNotification mbsn = (MBeanServerNotification)notification;
      String                  type = notification.getType();
      ObjectName              name = mbsn.getMBeanName();
      
      if(type.equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
        if(SessionsHelper.getHelper().isSessionsProductMBean(name)) {
          try {
            addBean(name);
          } catch(IOException ioe) {
            ioe.printStackTrace();
          }
        }
      }
    }
  }

  private void initMenu() {
    m_refreshAction = new RefreshAction();

    m_popupMenu = new JPopupMenu("SessionNode Actions");
    m_popupMenu.add(m_refreshAction);

    addActionBinding(REFRESH_ACTION, m_refreshAction);
  }

  public JPopupMenu getPopupMenu() {
    return m_popupMenu;
  }

  public void refresh() {
    /**/
  }

  private class RefreshAction extends XAbstractAction {
    private RefreshAction() {
      super();

      setName(AdminClient.getContext().getMessage("refresh.name"));
      setSmallIcon(ClassesHelper.getHelper().getRefreshIcon());
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
    }

    public void actionPerformed(ActionEvent ae) {
      refresh();
    }
  }

  public void nodeClicked(MouseEvent me) {
    m_refreshAction.actionPerformed(null);
  }
}
