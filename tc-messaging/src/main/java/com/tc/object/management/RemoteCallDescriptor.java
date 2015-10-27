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
