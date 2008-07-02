/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.common.XObjectTableModel;
import com.tc.admin.model.IClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.event.TableModelEvent;

public class ClientTableModel extends XObjectTableModel {
  private static final String[] FIELDS  = { "Host", "Port", "ChannelID", "LiveObjectCount" };

  private static final String[] HEADERS = { AdminClient.getContext().getMessage("dso.client.host"),
      AdminClient.getContext().getMessage("dso.client.port"),
      AdminClient.getContext().getMessage("dso.client.channelID"),
      AdminClient.getContext().getMessage("dso.client.liveObjectCount") };

  public ClientTableModel() {
    super(ClientWrapper.class, FIELDS, HEADERS);
  }

  void setClients(IClient[] clients) {
    List<ClientWrapper> wrappers = new ArrayList<ClientWrapper>();
    for (IClient client : clients) {
      wrappers.add(new ClientWrapper(client));
    }
    set(wrappers);
  }

  void addClient(IClient client) {
    add(new ClientWrapper(client));
    int row = getRowCount() - 1;
    fireTableRowsInserted(row, row);
  }

  void removeClient(IClient client) {
    int count = getRowCount();
    for (int i = 0; i < count; i++) {
      ClientWrapper wrapper = (ClientWrapper) getObjectAt(i);
      if (wrapper.m_client == client) {
        remove(i);
        fireTableRowsDeleted(i, i);
        return;
      }
    }
  }

  void updateObjectCounts(Map<IClient, Integer> map) {
    int rows = getRowCount();
    for (int i = 0; i < rows; i++) {
      ClientWrapper wrapper = (ClientWrapper) getObjectAt(i);
      wrapper.m_liveObjectCount = map.get(wrapper.m_client);
    }
    fireTableChanged(new TableModelEvent(this, 0, rows - 1, 3, TableModelEvent.UPDATE));

  }

  public class ClientWrapper {
    private IClient m_client;
    private String  m_host;
    private int     m_port;
    private long    m_channelID;
    private int     m_liveObjectCount;

    private ClientWrapper(IClient client) {
      m_client = client;
      m_host = client.getHost();
      m_port = client.getPort();
      m_channelID = client.getChannelID();
      m_liveObjectCount = client.getLiveObjectCount();
    }

    public String getHost() {
      return m_host;
    }

    public int getPort() {
      return m_port;
    }

    public long getChannelID() {
      return m_channelID;
    }

    public int getLiveObjectCount() {
      return m_liveObjectCount;
    }
  }
}
