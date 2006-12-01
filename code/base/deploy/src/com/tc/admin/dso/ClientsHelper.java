/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.BaseHelper;
import com.tc.admin.ConnectionContext;

import java.io.IOException;
import java.net.URL;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public class ClientsHelper extends BaseHelper {
  private static ClientsHelper m_helper = new ClientsHelper();
  private Icon                 m_clientsIcon;
  private Icon                 m_clientIcon;

  public static ClientsHelper getHelper() {
    return m_helper;
  }

  public Icon getClientsIcon() {
    if(m_clientsIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"hierarchicalLayout.gif");
      m_clientsIcon = new ImageIcon(url);
    }

    return m_clientsIcon;
  }

  public Icon getClientIcon() {
    if(m_clientIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"genericvariable_obj.gif");
      m_clientIcon = new ImageIcon(url);
    }

    return m_clientIcon;
  }

  public DSOClient[] getClients(ConnectionContext cc)
    throws MBeanException,
           AttributeNotFoundException,
           InstanceNotFoundException,
           ReflectionException,
           IOException
  {
    ObjectName[] clientNames = getClientNames(cc);
    int          count       = (clientNames != null) ? clientNames.length : 0;
    DSOClient[]  result      = new DSOClient[count];

    for(int i = 0; i < count; i++) {
      result[i] = new DSOClient(cc, clientNames[i]);
    }

    return result;
  }

  public ObjectName[] getClientNames(ConnectionContext cc)
    throws MBeanException,
           AttributeNotFoundException,
           InstanceNotFoundException,
           ReflectionException,
           IOException
  {
    ObjectName dso = DSOHelper.getHelper().getDSOMBean(cc);
    return (ObjectName[])cc.getAttribute(dso, "Clients");
  }
}
