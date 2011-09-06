/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.PrefsHelper;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;

import java.util.prefs.Preferences;

public class NavTreeModel extends XTreeModel {
  private static final String SERVERS      = ServersHelper.SERVERS;
  private static final String NAME         = ServersHelper.NAME;
  private static final String HOST         = ServersHelper.HOST;
  private static final String PORT         = ServersHelper.PORT;
  private static final String AUTO_CONNECT = ServersHelper.AUTO_CONNECT;

  public NavTreeModel(IAdminClientContext adminClientContext) {
    super();

    Preferences prefs = adminClientContext.getPrefs();
    Preferences serverPrefs = prefs.node(SERVERS);
    PrefsHelper prefsHelper = PrefsHelper.getHelper();
    String[] children = prefsHelper.childrenNames(serverPrefs);
    int count = children.length;
    Preferences serverPref;
    ClusterNode serverNode;
    String name;
    String host;
    int port;
    boolean autoConnect;

    if (count > 0) {
      for (int i = 0; i < count; i++) {
        serverPref = serverPrefs.node(children[i]);
        name = serverPref.get(NAME, adminClientContext.getString("cluster.node.label"));
        host = serverPref.get(HOST, ConnectionContext.DEFAULT_HOST);
        port = serverPref.getInt(PORT, ConnectionContext.DEFAULT_PORT);
        autoConnect = serverPref.getBoolean(AUTO_CONNECT, ConnectionContext.DEFAULT_AUTO_CONNECT);
        serverNode = adminClientContext.getNodeFactory().createClusterNode(adminClientContext, host, port, autoConnect);
        serverNode.setClusterName(name);

        insertNodeInto(serverNode, (XTreeNode) getRoot(), i);
      }
    } else {
      serverNode = adminClientContext.getNodeFactory().createClusterNode(adminClientContext);
      serverNode.setPreferences(serverPrefs.node("server-0"));
      adminClientContext.storePrefs();
      insertNodeInto(serverNode, (XTreeNode) getRoot(), 0);
    }
  }
}
