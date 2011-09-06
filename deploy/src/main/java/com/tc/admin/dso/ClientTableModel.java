/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XObjectTableModel;
import com.tc.admin.model.IClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.event.TableModelEvent;

public class ClientTableModel extends XObjectTableModel {
  private static final String[] FIELDS  = { "Host", "Port", "ChannelID", "LiveObjectCount" };

  private static final String[] HEADERS = { "dso.client.host", "dso.client.port", "dso.client.clientID",
      "live.object.count"              };

  public ClientTableModel(ApplicationContext appContext) {
    super(ClientWrapper.class, FIELDS, appContext.getMessages(HEADERS));
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
      if (wrapper.client == client) {
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
      Integer value = map.get(wrapper.client);
      if (value != null) {
        wrapper.liveObjectCount = value.intValue();
      }
    }
    fireTableChanged(new TableModelEvent(this, 0, rows - 1, 3, TableModelEvent.UPDATE));
  }

  public static class ClientWrapper {
    private final IClient client;
    private final String  host;
    private final int     port;
    private final long    channelID;
    private int           liveObjectCount;

    private ClientWrapper(IClient client) {
      this.client = client;
      host = client.getHost();
      port = client.getPort();
      channelID = client.getChannelID();
      liveObjectCount = client.getLiveObjectCount();
    }

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }

    public long getChannelID() {
      return channelID;
    }

    public int getLiveObjectCount() {
      return liveObjectCount;
    }
  }
}
