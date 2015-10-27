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
package com.tc.object.management;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.NodeID;

import java.io.IOException;

/**
 *
 */
public class RemoteCallHolder extends RemoteCallDescriptor {

  private byte[] serializedArgs;

  public RemoteCallHolder() {
  }

  public RemoteCallHolder(RemoteCallDescriptor remoteCallDescriptor, Object[] args) {
    super(remoteCallDescriptor.getL1Node(), remoteCallDescriptor.getServiceID(), remoteCallDescriptor.getMethodName(), remoteCallDescriptor.getArgTypeNames());
    this.serializedArgs = SerializationHelper.serialize(args);
  }

  protected RemoteCallHolder(NodeID l1Node, ServiceID serviceID, String methodName, String[] argTypeNames, Object[] args) {
    super(l1Node, serviceID, methodName, argTypeNames);
    this.serializedArgs = SerializationHelper.serialize(args);
  }

  public Object[] getArgs(ClassLoader classLoader) throws ClassNotFoundException {
    return (Object[])SerializationHelper.deserialize(serializedArgs, classLoader);
  }

  public void setArgs(Object[] args) {
    this.serializedArgs = SerializationHelper.serialize(args);
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
      super.serializeTo(serialOutput);
    if (serializedArgs == null) {
      serialOutput.writeInt(0);
    } else {
      serialOutput.writeInt(serializedArgs.length);
      serialOutput.write(serializedArgs);
    }
  }

  @Override
  public RemoteCallHolder deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    super.deserializeFrom(serialInput);
    int arraySize = serialInput.readInt();
    serializedArgs = new byte[arraySize];
    serialInput.readFully(serializedArgs);
    return this;
  }
}
