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
 *
 */
package com.tc.l2.api;

import com.tc.l2.state.StateChangeListener;
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

}
