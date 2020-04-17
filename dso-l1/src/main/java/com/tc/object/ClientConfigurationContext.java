/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 */
package com.tc.object;

import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;


public class ClientConfigurationContext extends ConfigurationContextImpl {
  public static final String             VOLTRON_ENTITY_MULTI_RESPONSE_STAGE                      = "multi_request_ack_stage";

  public static final int                MAX_PENDING_REQUESTS                        = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.CLIENT_MAX_PENDING_REQUESTS, 5000);
  
  public ClientConfigurationContext(StageManager stageManager) {
    super(stageManager);
  }
}
