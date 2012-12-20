/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.messaging.http;

import org.apache.commons.io.IOUtils;
import org.terracotta.groupConfigForL1.ServerGroup;
import org.terracotta.groupConfigForL1.ServerGroupsDocument;
import org.terracotta.groupConfigForL1.ServerGroupsDocument.ServerGroups;
import org.terracotta.groupConfigForL1.ServerInfo;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GroupInfoServlet extends HttpServlet {
  public static final String                   GROUP_INFO_ATTRIBUTE = GroupInfoServlet.class.getName() + ".groupinfo";

  private volatile L2ConfigurationSetupManager configSetupManager;
  private ServerGroupsDocument                 serverGroupsDocument = null;
  private Map<String, Integer>                 serverNameToTsaPort;
  private Map<String, String>                  serverNameToHostName;

  @Override
  public void init() {
    // NO OP
  }

  private void createDocumentToSend() {
    configSetupManager = (L2ConfigurationSetupManager) getServletContext().getAttribute(GROUP_INFO_ATTRIBUTE);
    serverGroupsDocument = ServerGroupsDocument.Factory.newInstance();
    createServerNameToTsaPortAndHostname();
    ServerGroups serverGroups = serverGroupsDocument.addNewServerGroups();
    List<ActiveServerGroupConfig> activeServerGroupConfigs = configSetupManager.activeServerGroupsConfig()
        .getActiveServerGroups();
    for (ActiveServerGroupConfig activeServerGroupConfig : activeServerGroupConfigs) {
      addServerGroup(serverGroups, activeServerGroupConfig);
    }
  }

  private void createServerNameToTsaPortAndHostname() {
    serverNameToTsaPort = new HashMap<String, Integer>();
    serverNameToHostName = new HashMap<String, String>();
    String[] allServerNames = configSetupManager.allCurrentlyKnownServers();
    for (String allServerName : allServerNames) {
      int port = 0;
      String host = null;
      try {
        port = configSetupManager.dsoL2ConfigFor(allServerName).tsaPort().getIntValue();
        host = configSetupManager.dsoL2ConfigFor(allServerName).host();
      } catch (ConfigurationSetupException e) {
        throw new RuntimeException(e);
      }
      serverNameToTsaPort.put(allServerName, port);
      serverNameToHostName.put(allServerName, host);
    }
  }

  protected void addServerGroup(ServerGroups serverGroups, ActiveServerGroupConfig activeServerGroupConfig) {
    ServerGroup group = serverGroups.addNewServerGroup();
    group.setGroupName(activeServerGroupConfig.getGroupName());
    String[] members = activeServerGroupConfig.getMembers();
    for (String member : members) {
      ServerInfo serverInfo = group.addNewServerInfo();
      serverInfo.setTsaPort(new BigInteger(serverNameToTsaPort.get(member).intValue() + ""));
      serverInfo.setName(serverNameToHostName.get(member));
    }
  }

  @Override
  protected synchronized void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    createDocumentToSend();
    OutputStream out = response.getOutputStream();
    IOUtils.copy(this.serverGroupsDocument.newInputStream(), out);
    response.flushBuffer();
  }
}
