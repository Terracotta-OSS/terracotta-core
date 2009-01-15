/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import static com.tc.admin.model.IClusterNode.POLLED_ATTR_LIVE_OBJECT_COUNT;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.PolledAttributeListener;
import com.tc.admin.model.PolledAttributesResult;
import com.tc.admin.model.IClusterModel.PollScope;

import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class ClientsPanel extends XContainer implements PolledAttributeListener, HierarchyListener {
  protected IClusterModel clusterModel;
  protected ClientsTable  table;

  public ClientsPanel(ApplicationContext appContext, IClusterModel clusterModel, IClient[] clients) {
    super(new BorderLayout());
    this.clusterModel = clusterModel;
    add(new JScrollPane(table = new ClientsTable(appContext)));
    setClients(clients);
    addHierarchyListener(this);
  }

  public void setClients(IClient[] clients) {
    for (IClient client : clients) {
      add(client);
    }
  }

  public boolean haveAnyClients() {
    return table.getModel().getRowCount() > 0;
  }

  public void add(IClient client) {
    table.addClient(client);
  }

  public void remove(IClient client) {
    table.removeClient(client);
  }

  public void attributesPolled(PolledAttributesResult result) {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel == null) return;
    final Map<IClient, Integer> locMap = new HashMap<IClient, Integer>();
    for (IClient client : clusterModel.getClients()) {
      Number value = (Number) result.getPolledAttribute(client, POLLED_ATTR_LIVE_OBJECT_COUNT);
      if (value != null) {
        locMap.put(client, Integer.valueOf(value.intValue()));
      }
    }
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        table.updateObjectCounts(locMap);
      }
    });
  }

  private void addPolledAttributeListeners() {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel != null) {
      theClusterModel.addPolledAttributeListener(PollScope.CLIENTS, POLLED_ATTR_LIVE_OBJECT_COUNT, this);
    }
  }

  private void removePolledAttributeListeners() {
    IClusterModel theClusterModel = getClusterModel();
    if (theClusterModel != null) {
      theClusterModel.removePolledAttributeListener(PollScope.CLIENTS, POLLED_ATTR_LIVE_OBJECT_COUNT, this);
    }
  }

  private synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  public void tearDown() {
    removePolledAttributeListeners();

    super.tearDown();

    synchronized (this) {
      clusterModel = null;
      table = null;
    }
  }

  public void hierarchyChanged(HierarchyEvent e) {
    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
      if (isShowing()) {
        addPolledAttributeListeners();
      } else {
        removePolledAttributeListeners();
      }
    }
  }
}
