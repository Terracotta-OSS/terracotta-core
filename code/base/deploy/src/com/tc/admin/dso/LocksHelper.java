/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.BaseHelper;
import com.tc.admin.ConnectionContext;
import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.objectserver.lockmanager.api.LockMBean;
import com.tc.objectserver.lockmanager.api.DeadlockChain;

import java.io.IOException;
import java.net.URL;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public class LocksHelper extends BaseHelper {
  private static LocksHelper m_helper = new LocksHelper();
  private Icon               m_locksIcon;
  private Icon               m_lockIcon;
  private Icon               m_detectDeadlocksIcon;

  public static LocksHelper getHelper() {
    return m_helper;
  }

  public Icon getLocksIcon() {
    if(m_locksIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"owned_monitor_obj.gif");
      m_locksIcon = new ImageIcon(url);
    }

    return m_locksIcon;
  }

  public Icon getLockIcon() {
    if(m_lockIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"deadlock_view.gif");
      m_lockIcon = new ImageIcon(url);
    }

    return m_lockIcon;
  }

  public Icon getDetectDeadlocksIcon() {
    if(m_detectDeadlocksIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"insp_sbook.gif");
      m_detectDeadlocksIcon = new ImageIcon(url);
    }

    return m_detectDeadlocksIcon;
  }

  public LockMBean[] getLocks(ConnectionContext cc)
    throws MBeanException,
           AttributeNotFoundException,
           InstanceNotFoundException,
           ReflectionException,
           IOException
  {
    ObjectName dso = DSOHelper.getHelper().getDSOMBean(cc);
    return (LockMBean[])cc.getAttribute(dso, "Locks");
  }

  public void detectDeadlocks(ConnectionContext cc) {
    try {
      ObjectName      dso    = DSOHelper.getHelper().getDSOMBean(cc);
      String          op     = "scanForDeadLocks";
      Object[]        args   = new Object[] {};
      String[]        types  = new String[] {};
      DeadlockChain[] chains = (DeadlockChain[])cc.invoke(dso, op, args, types);
      StringBuffer    sb     = new StringBuffer();

      if(chains != null) {
        DeadlockChain chainRoot;
        DeadlockChain chain;

        for(int i = 0; i < chains.length; i++) {
          chainRoot = chains[i];
          chain     = null;

          while(chainRoot != chain) {
            if(chain == null) {
              chain = chainRoot;
            }

            sb.append(chain.getWaiter());
            sb.append(" waiting on ");
            sb.append(chain.getWaitingOn());
            sb.append(System.getProperty("line.separator"));
            
            chain = chain.getNextLink();
          }
        }

        AdminClientContext acc = AdminClient.getContext();
        acc.controller.log(sb.toString());
      }
    }
    catch(Exception e) {
      AdminClient.getContext().log(e);
    }
  }
}
