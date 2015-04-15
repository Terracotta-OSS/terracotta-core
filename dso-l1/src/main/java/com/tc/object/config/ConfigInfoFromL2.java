/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
