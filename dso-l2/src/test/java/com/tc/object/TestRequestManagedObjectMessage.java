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
package com.tc.object;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.RequestManagedObjectMessage;
import com.tc.util.ObjectIDSet;

public class TestRequestManagedObjectMessage implements RequestManagedObjectMessage {

  private ObjectIDSet removed;
  private ObjectIDSet objectIDs;

  public TestRequestManagedObjectMessage() {
    super();
  }

  @Override
  public ObjectRequestID getRequestID() {
    return null;
  }

  @Override
  public ObjectIDSet getRequestedObjectIDs() {
    return this.objectIDs;
  }

  public void setObjectIDs(ObjectIDSet IDs) {
    this.objectIDs = IDs;
  }

  @Override
  public ObjectIDSet getRemoved() {
    return this.removed;
  }

  public void setRemoved(ObjectIDSet rm) {
    this.removed = rm;
  }

  @Override
  public void initialize(ObjectRequestID rID, ObjectIDSet requestedObjectIDs, int requestDepth,
                         ObjectIDSet removeObjects) {
    //
  }

  @Override
  public void send() {
    //
  }

  @Override
  public MessageChannel getChannel() {
    return null;
  }

  @Override
  public NodeID getSourceNodeID() {
    return new ClientID(0);
  }

  @Override
  public int getRequestDepth() {
    return 400;
  }

  @Override
  public void recycle() {
    return;
  }

  @Override
  public String getRequestingThreadName() {
    return "TestThreadDummy";
  }

  @Override
  public LOOKUP_STATE getLookupState() {
    return LOOKUP_STATE.CLIENT;
  }

  @Override
  public ClientID getClientID() {
    return new ClientID(0);
  }

  @Override
  public Object getKey() {
    return null;
  }

}
