/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.l2.api;

import com.tc.l2.state.StateManager;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupManager;
import com.tc.text.PrettyPrintable;

public interface L2Coordinator extends GroupEventsListener, PrettyPrintable {

  public void start();

  public ReplicatedClusterStateManager getReplicatedClusterStateManager();

  public StateManager getStateManager();

  public GroupManager<AbstractGroupMessage> getGroupManager();

  public void shutdown();
}
