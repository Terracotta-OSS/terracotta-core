/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.messaging.http;

import org.apache.commons.io.IOUtils;
import org.terracotta.groupConfigForL1.ServerGroup;
import org.terracotta.groupConfigForL1.ServerGroupsDocument;
import org.terracotta.groupConfigForL1.ServerInfo;
import org.terracotta.groupConfigForL1.ServerGroupsDocument.ServerGroups;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GroupInfoServlet extends HttpServlet {
  public static final String                      GROUP_INFO_ATTRIBUTE = GroupInfoServlet.class.getName()
                                                                         + ".groupinfo";

  private volatile L2ConfigurationSetupManager configSetupManager;
  private ServerGroupsDocument                    serverGroupsDocument = null;
  private Map<String, Integer>                    serverNameToDsoPort;
  private Map<String, String>                     serverNameToHostName;

  public void init() {
    // NO OP
  }

  private void createDocumentToSend() {
    configSetupManager = (L2ConfigurationSetupManager) getServletContext().getAttribute(GROUP_INFO_ATTRIBUTE);
    serverGroupsDocument = ServerGroupsDocument.Factory.newInstance();
    createServerNameToDsoPortAndHostname();
    ServerGroups serverGroups = serverGroupsDocument.addNewServerGroups();
    ActiveServerGroupConfig[] activeServerGroupConfigs = configSetupManager.activeServerGroupsConfig()
        .getActiveServerGroupArray();
    for (int i = 0; i < activeServerGroupConfigs.length; i++) {
      addServerGroup(serverGroups, activeServerGroupConfigs[i]);
    }
  }

  private void createServerNameToDsoPortAndHostname() {
    serverNameToDsoPort = new HashMap<String, Integer>();
    serverNameToHostName = new HashMap<String, String>();
    String[] allServerNames = configSetupManager.allCurrentlyKnownServers();
    for (int i = 0; i < allServerNames.length; i++) {
      int port = 0;
      String host = null;
      try {
        port = configSetupManager.dsoL2ConfigFor(allServerNames[i]).dsoPort().getIntValue();
        host = configSetupManager.dsoL2ConfigFor(allServerNames[i]).host();
      } catch (ConfigurationSetupException e) {
        throw new RuntimeException(e);
      }
      serverNameToDsoPort.put(allServerNames[i], port);
      serverNameToHostName.put(allServerNames[i], host);
    }
  }

  protected void addServerGroup(ServerGroups serverGroups, ActiveServerGroupConfig activeServerGroupConfig) {
    ServerGroup group = serverGroups.addNewServerGroup();
    group.setGroupName(activeServerGroupConfig.getGroupName());
    String[] members = activeServerGroupConfig.getMembers().getMemberArray();
    for (int i = 0; i < members.length; i++) {
      ServerInfo serverInfo = group.addNewServerInfo();
      serverInfo.setDsoPort(new BigInteger(serverNameToDsoPort.get(members[i]).intValue() + ""));
      serverInfo.setName(serverNameToHostName.get(members[i]));
    }
  }

  protected synchronized void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    createDocumentToSend();
    OutputStream out = response.getOutputStream();
    IOUtils.copy(this.serverGroupsDocument.newInputStream(), out);
    response.flushBuffer();
  }
}
