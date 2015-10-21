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
package com.tc.net.groups;

import com.tc.io.TCSerializable;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessageImpl;

public interface GroupMessage extends TCSerializable<GroupMessage> {

  public int getType();
    
  public abstract MessageID getMessageID();

  public abstract MessageID inResponseTo();

  public abstract void setMessageOrginator(NodeID n);

  public abstract NodeID messageFrom();
  
  public boolean isRecycleOnRead(TCMessageImpl message);

}
