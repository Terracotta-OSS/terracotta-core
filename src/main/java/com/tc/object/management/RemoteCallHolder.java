/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

  public Object[] getArgs(final ClassLoader classLoader) throws ClassNotFoundException {
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
  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    super.deserializeFrom(serialInput);
    int arraySize = serialInput.readInt();
    serializedArgs = new byte[arraySize];
    serialInput.readFully(serializedArgs);
    return this;
  }
}
