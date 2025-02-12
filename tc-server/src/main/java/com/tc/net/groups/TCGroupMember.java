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
package com.tc.net.groups;

import com.tc.net.ServerID;
import com.tc.net.protocol.tcm.MessageChannel;

public interface TCGroupMember {

  public ServerID getLocalNodeID();

  public ServerID getPeerNodeID();

  public MessageChannel getChannel();

  public void send(AbstractGroupMessage msg, Runnable sentCallback) throws GroupException;
  
  public void sendIgnoreNotReady(AbstractGroupMessage msg);

  public void setTCGroupManager(TCGroupManagerImpl manager);

  public TCGroupManagerImpl getTCGroupManager();

  public boolean isReady();

  public void setReady(boolean isReady);

  public boolean isJoinedEventFired();

  public void setJoinedEventFired(boolean isReady);

  public void close();

  public boolean isHighPriorityNode();

  public void memberAddingInProcess();

  public void abortMemberAdding();

  public void notifyMemberAdded();
}