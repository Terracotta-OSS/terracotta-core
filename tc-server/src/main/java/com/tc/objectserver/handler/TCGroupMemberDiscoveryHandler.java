/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.net.groups.DiscoveryStateMachine;
import com.tc.net.groups.TCGroupManagerImpl;

public class TCGroupMemberDiscoveryHandler extends AbstractEventHandler<DiscoveryStateMachine> {
  private final TCGroupManagerImpl manager;
  
  public TCGroupMemberDiscoveryHandler(TCGroupManagerImpl manager) {
    this.manager = manager;
  }
  
  @Override
  public void handleEvent(DiscoveryStateMachine context) {
    manager.getDiscover().discoveryHandler(context);
  }

  @Override
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
  }

}