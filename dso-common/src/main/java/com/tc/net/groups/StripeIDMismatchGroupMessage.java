/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.GroupID;

import java.io.IOException;

public class StripeIDMismatchGroupMessage extends AbstractGroupMessage {

  public static final int ERROR_STRIPEID_MISMATCH  = 0;
  public static final int ERROR_NOT_CLUSTER_MEMBER = 1;
  public static final int ERROR_MISMATCH_STRIPEID  = 2;
  public static final int ERROR_MISMATCH_GROUPID   = 3;
  public static final int MISMATCH_TEMPORARY       = 4;
  public static final int MISMATCH_NOT_READY_YET   = 5;

  private int             errorType;
  private String          reason;
  private GroupID         groupID;

  // To make serialization happy
  public StripeIDMismatchGroupMessage() {
    super(-1);
  }

  public StripeIDMismatchGroupMessage(int type, int errorType, String reason, GroupID groupID) {
    super(type);
    this.reason = reason;
    this.errorType = errorType;
    this.groupID = groupID;
  }

  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    errorType = in.readInt();
    reason = in.readString();
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer.deserializeFrom(in);
    groupID = (GroupID) nodeIDSerializer.getNodeID();
  }

  protected void basicSerializeTo(TCByteBufferOutput out) {
    out.writeInt(errorType);
    out.writeString(reason);
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer(groupID);
    nodeIDSerializer.serializeTo(out);
  }

  public String toString() {
    return "StripeIDMismatchGroupMessage [ " + errorType + " , " + reason + " , " + groupID + " ]";
  }

  public String getReason() {
    return reason;
  }

  public int getErrorType() {
    return errorType;
  }

  public GroupID getGroupID() {
    return groupID;
  }

}
