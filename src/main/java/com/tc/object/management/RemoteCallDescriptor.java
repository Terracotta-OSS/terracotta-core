/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.management;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.net.NodeID;
import com.tc.net.groups.NodeIDSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 */
public class RemoteCallDescriptor implements TCSerializable<RemoteCallDescriptor> {

  private NodeID l1Node;
  private ServiceID serviceID;
  private String    methodName;
  private String[]  argTypeNames;

  public RemoteCallDescriptor() {
  }

  public RemoteCallDescriptor(NodeID l1Node, ServiceID serviceID, String methodName, String[] argTypeNames) {
    this.l1Node = l1Node;
    this.serviceID = serviceID;
    this.methodName = methodName;
    this.argTypeNames = argTypeNames;
  }

  public NodeID getL1Node() {
    return l1Node;
  }

  public ServiceID getServiceID() {
    return serviceID;
  }

  public String getMethodName() {
    return methodName;
  }

  public String[] getArgTypeNames() {
    return argTypeNames;
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    new NodeIDSerializer(l1Node).serializeTo(serialOutput);
    serviceID.serializeTo(serialOutput);
    serialOutput.writeString(methodName);
    if (argTypeNames != null) {
      serialOutput.writeInt(argTypeNames.length);
      for (String argTypeName : argTypeNames) {
        serialOutput.writeString(argTypeName);
      }
    } else {
      serialOutput.writeInt(-1);
    }
  }

  @Override
  public RemoteCallDescriptor deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    l1Node = new NodeIDSerializer().deserializeFrom(serialInput).getNodeID();
    serviceID = new ServiceID().deserializeFrom(serialInput);
    methodName = serialInput.readString();
    int argTypeNamesCount = serialInput.readInt();
    if (argTypeNamesCount > -1) {
      ArrayList<String> argTypeNamesList = new ArrayList<String>(argTypeNamesCount);
      for (int i = 0; i < argTypeNamesCount; i++) {
        argTypeNamesList.add(serialInput.readString());
      }
      argTypeNames = argTypeNamesList.toArray(new String[argTypeNamesCount]);
    } else {
      argTypeNames = null;
    }
    return this;
  }

  @Override
  public String toString() {
    return "RemoteCallDescriptor of node " + l1Node + " serviceID " + serviceID + " method " + methodName + " with args " + Arrays.toString(argTypeNames);
  }
}
