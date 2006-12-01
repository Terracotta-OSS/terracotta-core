/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import org.dijon.ContainerResource;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;

import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;

public class ClientPanel extends XContainer {
  private DSOClient m_client;
  private XLabel    m_hostLabel;
  private XLabel    m_portLabel;
  private XLabel    m_channelIDLabel;

  public ClientPanel(DSOClient client) {
    super();

    AdminClientContext acc = AdminClient.getContext();

    load((ContainerResource)acc.topRes.getComponent("DSOClientPanel"));

    m_hostLabel      = (XLabel)findComponent("HostLabel");
    m_portLabel      = (XLabel)findComponent("PortLabel");
    m_channelIDLabel = (XLabel)findComponent("ChannelIDLabel");

    setClient(client);
  }

  public void setClient(DSOClient client) {
    m_client = client;

    setHost(client.getHost());
    setPort(client.getPort());
    setChannelID(client.getChannelID());
  }

  public DSOClient getClient() {
    return m_client;
  }

  public void setHost(String host) {
    m_hostLabel.setText(host);
  }

  public void setPort(int port) {
    m_portLabel.setText(port+"");
  }

  public void setChannelID(String channelID) {
    m_channelIDLabel.setText(channelID);
  }
}
