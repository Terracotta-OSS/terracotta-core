/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.admin.dso;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;

import javax.management.ObjectName;
import javax.management.j2ee.statistics.CountStatistic;

import com.tc.admin.ConnectionContext;
import com.tc.admin.AdminClient;

public class DSOClient {
  private   ConnectionContext     cc;
  private   ObjectName            bean;
  private   String                channelID;
  private   String                remoteAddress;
  private   String                host;
  private   Integer               port;
  protected PropertyChangeSupport changeHelper;

  private static final String CHANNEL_ID_PROPERTY = "channelID";

  public DSOClient(ConnectionContext cc, ObjectName bean) {
    this.cc        = cc;
    this.bean      = bean;
    this.channelID = bean.getKeyProperty(CHANNEL_ID_PROPERTY);
    changeHelper   = new PropertyChangeSupport(this);
  }

  public ObjectName getObjectName() {
    return bean;
  }

  public String getChannelID() {
    return channelID;
  }

  public String getRemoteAddress() {
    if(remoteAddress == null) {
      try {
        remoteAddress = (String)cc.getAttribute(bean, "RemoteAddress");
      }
      catch(Exception e) {
        AdminClient.getContext().log(e);
      }
    }

    return remoteAddress;
  }

  public String getHost() {
    if(host == null) {
      host = "unknown";
      
      String addr = getRemoteAddress();
      if(addr != null && addr.indexOf(':') != -1) {
        host = addr.substring(0, addr.lastIndexOf(':'));
      }
    }

    return host;
  }

  public int getPort() {
    if(port == null) {
      port = new Integer(-1);

      String addr = getRemoteAddress();
      if(addr != null && addr.indexOf(":") != -1) {
        try {
          port = new Integer(addr.substring(addr.lastIndexOf(':')+1));
        } catch(Exception e) {/**/}
      }
    }

    return port.intValue();
  }

  public void refresh() {
    try {
      cc.invoke(bean, "refresh", new Object[]{}, new String[]{});

      changeHelper.firePropertyChange(
        new PropertyChangeEvent(this, null, null, null));
    }
    catch(Exception e) {
      AdminClient.getContext().log(e);
    }
  }

  public String toString() {
    return getRemoteAddress();
  }

  public CountStatistic getObjectFlushRate() {
    try {
      return (CountStatistic)cc.getAttribute(bean, "ObjectFlushRate");
    }
    catch(Exception e) {
      AdminClient.getContext().log(e);
    }

    return null;
  }

  public CountStatistic getObjectFaultRate() {
    try {
      return (CountStatistic)cc.getAttribute(bean, "ObjectFaultRate");
    }
    catch(Exception e) {
      AdminClient.getContext().log(e);
    }

    return null;
  }

  public CountStatistic getTransactionRate() {
    try {
      return (CountStatistic)cc.getAttribute(bean, "TransactionRate");
    }
    catch(Exception e) {
      AdminClient.getContext().log(e);
    }

    return null;
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    changeHelper.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    changeHelper.removePropertyChangeListener(listener);
  }
}
