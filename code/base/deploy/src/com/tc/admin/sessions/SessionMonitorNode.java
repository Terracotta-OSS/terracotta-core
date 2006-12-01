/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.sessions;

import com.tc.admin.AdminClient;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.dso.ClassesHelper;
import com.tc.management.beans.sessions.SessionMonitorMBean;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultTreeModel;

public class SessionMonitorNode extends ComponentNode implements NotificationListener {
  ObjectName            m_beanName;
  SessionMonitorMBean   m_bean;
  private JPopupMenu    m_popupMenu;
  private RefreshAction m_refreshAction;

  private static final String REFRESH_ACTION = "RefreshAction";
  
  public SessionMonitorNode(ConnectionContext cc, SessionMonitorMBean bean, ObjectName beanName) {
    super();

    try {
      ObjectName mbsd = cc.queryName("JMImplementation:type=MBeanServerDelegate");
      
      if(mbsd != null) {
        cc.addNotificationListener(mbsd, this);
      }
    } catch(Exception ioe) {
      ioe.printStackTrace();
    }
    
    m_beanName = beanName;
    m_bean     = bean;
    
    setLabel(beanName.getKeyProperty("node"));
    setComponent(new SessionMonitorPanel(bean));

    initMenu();
  }

  public void handleNotification(Notification notification, Object handback) {
    if(notification instanceof MBeanServerNotification) {
      MBeanServerNotification mbsn = (MBeanServerNotification)notification;
      String                  type = notification.getType();
      ObjectName              name = mbsn.getMBeanName();
      
      if(type.equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
        if(name.equals(m_beanName)) {
          ((DefaultTreeModel)getModel()).removeNodeFromParent(SessionMonitorNode.this);
        }
      }
    }
  }
  
  private void initMenu() {
    m_refreshAction = new RefreshAction();

    m_popupMenu = new JPopupMenu("SessionMonitor Actions");
    m_popupMenu.add(m_refreshAction);

    addActionBinding(REFRESH_ACTION, m_refreshAction);
  }

  public JPopupMenu getPopupMenu() {
    return m_popupMenu;
  }

  public void refresh() {
    ((SessionMonitorPanel)getComponent()).refresh();
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
