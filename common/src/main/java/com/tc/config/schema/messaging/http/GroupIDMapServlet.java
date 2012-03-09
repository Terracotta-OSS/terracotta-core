/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.messaging.http;

import org.apache.commons.io.IOUtils;
import org.terracotta.groupConfigForL1.GroupnameId;
import org.terracotta.groupConfigForL1.GroupnameIdMapDocument;
import org.terracotta.groupConfigForL1.GroupnameIdMapDocument.GroupnameIdMap;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GroupIDMapServlet extends HttpServlet {
  public static final String GROUPID_MAP_ATTRIBUTE = GroupIDMapServlet.class.getName() + ".groupidmap";

  @Override
  protected synchronized void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    L2ConfigurationSetupManager configSetupManager = (L2ConfigurationSetupManager) getServletContext()
        .getAttribute(GROUPID_MAP_ATTRIBUTE);
    List<ActiveServerGroupConfig> activeServerGroupConfigs = configSetupManager.activeServerGroupsConfig()
        .getActiveServerGroups();
    GroupnameIdMapDocument groupnameIdMapDocument = GroupnameIdMapDocument.Factory.newInstance();
    GroupnameIdMap groupnameIdMap = groupnameIdMapDocument.addNewGroupnameIdMap();
    for (ActiveServerGroupConfig group : activeServerGroupConfigs) {
      GroupnameId groupnameId = groupnameIdMap.addNewGroupnameId();
      groupnameId.setName(group.getGroupName());
      groupnameId.setGid(new BigInteger(String.valueOf(group.getGroupId().toInt())));
    }

    OutputStream out = response.getOutputStream();
    IOUtils.copy(groupnameIdMapDocument.newInputStream(), out);

    response.flushBuffer();
  }
}
