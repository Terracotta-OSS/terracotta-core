/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import java.util.prefs.Preferences;

import com.tc.admin.ConnectionContext;

import com.tc.admin.common.PrefsHelper;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;

public class NavTreeModel extends XTreeModel {
  private static final String SERVERS      = ServersHelper.SERVERS;
  private static final String HOST         = ServersHelper.HOST;
  private static final String PORT         = ServersHelper.PORT;
  private static final String AUTO_CONNECT = ServersHelper.AUTO_CONNECT;

  public NavTreeModel() {
    super();

    AdminClientContext acc         = AdminClient.getContext();
    Preferences        prefs       = acc.prefs.node("AdminClient");
    Preferences        serverPrefs = prefs.node(SERVERS);
    PrefsHelper        prefsHelper = PrefsHelper.getHelper();
    String[]           children    = prefsHelper.childrenNames(serverPrefs);
    int                count       = children.length;
    Preferences        serverPref;
    ServerNode         serverNode;
    String             host;
    int                port;
    boolean            autoConnect;

    if(count > 0) {
      for(int i = 0; i < count; i++) {
        serverPref  = serverPrefs.node(children[i]);
        host        = serverPref.get(HOST, ConnectionContext.DEFAULT_HOST);
        port        = serverPref.getInt(PORT, ConnectionContext.DEFAULT_PORT);
        autoConnect = serverPref.getBoolean(AUTO_CONNECT, ConnectionContext.DEFAULT_AUTO_CONNECT);
        serverNode  = new ServerNode(host, port, autoConnect);

        insertNodeInto(serverNode, (XTreeNode)getRoot(), i);
      }
    }
    else {
      serverNode = new ServerNode();
      serverNode.setPreferences(serverPrefs.node("server-0"));

      acc.client.storePrefs();

      insertNodeInto(new ServerNode(), (XTreeNode)getRoot(), 0);
    }
  }
}
