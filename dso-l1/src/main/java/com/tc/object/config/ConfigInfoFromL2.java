/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

import org.terracotta.groupConfigForL1.ServerGroupsDocument;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.net.GroupID;
import com.terracottatech.config.L1ReconnectPropertiesDocument;

import java.util.Map;

public interface ConfigInfoFromL2 {

  public ServerGroupsDocument getServerGroupsFromL2() throws ConfigurationSetupException;

  public L1ReconnectPropertiesDocument getL1ReconnectPropertiesFromL2() throws ConfigurationSetupException;

  public Map<String, GroupID> getGroupNameIDMapFromL2() throws ConfigurationSetupException;
}
