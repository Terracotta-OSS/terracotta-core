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
package com.tc.net.groups;

import com.tc.net.ServerID;
import com.tc.net.protocol.tcm.MessageChannel;

public interface TCGroupMember {

  public ServerID getLocalNodeID();

  public ServerID getPeerNodeID();

  public MessageChannel getChannel();

  public void send(GroupMessage msg) throws GroupException;
  
  public void sendIgnoreNotReady(GroupMessage msg);

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