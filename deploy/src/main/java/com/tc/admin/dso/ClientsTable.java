/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.model.IClient;

import java.util.Map;

public class ClientsTable extends XObjectTable {
  private ClientTableModel model;

  public ClientsTable(ApplicationContext appContext) {
    super();
    setModel(model = new ClientTableModel(appContext));
    getColumnModel().getColumn(1).setCellRenderer(new XObjectTable.PortNumberRenderer());
    setSortDirection(UP);
    setSortColumn(3 /* LiveObjectCount */);
  }

  public void addClient(IClient client) {
    model.addClient(client);
  }

  public void removeClient(IClient client) {
    model.removeClient(client);
  }

  void updateObjectCounts(Map<IClient, Integer> map) {
    model.updateObjectCounts(map);
    sort();
  }
}
