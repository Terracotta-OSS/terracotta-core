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
package com.tc.net.groups;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.ServerID;

import java.io.IOException;

public abstract class AbstractGroupMessage implements GroupMessage {

  private static long      nextID           = 0;

  private int              type;
  private MessageID        id;
  private MessageID        requestID;

  private transient ServerID messageOrginator = ServerID.NULL_ID;

  protected AbstractGroupMessage(int type) {
    this.type = type;
    this.id = getNextID();
    this.requestID = MessageID.NULL_ID;
  }

  protected AbstractGroupMessage(int type, MessageID requestID) {
    this.type = type;
    this.id = getNextID();
    this.requestID = requestID;
  }

  @Override
  final public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(this.type);
    serialOutput.writeLong(this.id.toLong());
    serialOutput.writeLong(this.requestID.toLong());
    basicSerializeTo(serialOutput);
  }

  @Override
  final public GroupMessage deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    this.type = serialInput.readInt();
    this.id = new MessageID(serialInput.readLong());
    this.requestID = new MessageID(serialInput.readLong());
    basicDeserializeFrom(serialInput);
    return this;
  }

  abstract protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException;

  abstract protected void basicSerializeTo(TCByteBufferOutput out);

  private static final synchronized MessageID getNextID() {
    return new MessageID(nextID++);
  }

  @Override
  public int getType() {
    return this.type;
  }

  @Override
  public MessageID getMessageID() {
    return this.id;
  }

  @Override
  public MessageID inResponseTo() {
    return this.requestID;
  }

  @Override
  public void setMessageOrginator(ServerID n) {
    this.messageOrginator = n;
  }

  @Override
  public ServerID messageFrom() {
    return this.messageOrginator;
  }
}
