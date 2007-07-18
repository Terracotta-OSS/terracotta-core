/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class GroupZapNodeMessage extends AbstractGroupMessage {

  private static final TCLogger logger           = TCLogging.getLogger(GroupZapNodeMessage.class);

  public static final int       ZAP_NODE_REQUEST = 0;

  private int                   zapNodeType;
  private String                reason;
  private long[]                weights;
  private byte[]                instanceID;

  // To make serialization happy
  public GroupZapNodeMessage() {
    super(-1);
  }

  public GroupZapNodeMessage(int type, int zapNodeType, String reason, long[] weights, byte[] instanceID) {
    super(type);
    this.reason = reason;
    this.zapNodeType = zapNodeType;
    this.weights = weights;
    this.instanceID = instanceID;
  }

  protected void basicReadExternal(int msgType, ObjectInput in) throws IOException {
    Assert.assertEquals(ZAP_NODE_REQUEST, msgType);
    zapNodeType = in.readInt();
    reason = in.readUTF();
    weights = new long[in.readInt()];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = in.readLong();
    }
    int len = in.readInt();
    int off = 0;
    instanceID = new byte[len];
    while (len > 0) {
      int read = in.read(instanceID, off, len);
      if (read == -1) {
        logger.error("Error reading instance ID from input stream : " + read);
        break;
      }
      off += read;
      len -= read;
    }
  }

  protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
    Assert.assertEquals(ZAP_NODE_REQUEST, msgType);
    out.writeInt(zapNodeType);
    out.writeUTF(reason);
    out.writeInt(weights.length);
    for (int i = 0; i < weights.length; i++) {
      out.writeLong(weights[i]);
    }
    out.writeInt(instanceID.length);
    out.write(instanceID);
  }

  public String toString() {
    return "GroupZapNodeMessage [ " + zapNodeType + " , " + reason + " , weights = " + toString(weights)
           + " , instanceID = " + toString(instanceID) + " ]";
  }

  private String toString(byte[] bs) {
    if (bs == null) return "null";
    if (bs.length == 0) return "empty";
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < bs.length; i++) {
      sb.append(String.valueOf(bs[i])).append(",");
    }
    sb.setLength(sb.length() - 1);
    return sb.toString();
  }

  private String toString(long[] l) {
    if (l == null) return "null";
    if (l.length == 0) return "empty";
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < l.length; i++) {
      sb.append(String.valueOf(l[i])).append(",");
    }
    sb.setLength(sb.length() - 1);
    return sb.toString();
  }

  public String getReason() {
    return reason;
  }

  public int getZapNodeType() {
    return zapNodeType;
  }

  public long[] getWeights() {
    return weights;
  }

  public byte[] getInstanceID() {
    return instanceID;
  }

}
