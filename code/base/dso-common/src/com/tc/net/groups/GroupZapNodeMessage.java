/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class GroupZapNodeMessage extends AbstractGroupMessage {

  public static final int ZAP_NODE_REQUEST = 0;

  private int             zapNodeType;
  private String          reason;

  // To make serialization happy
  public GroupZapNodeMessage() {
    super(-1);
  }

  public GroupZapNodeMessage(int type, int zapNodeType, String reason) {
    super(type);
    this.reason = reason;
    this.zapNodeType = zapNodeType;
  }

  protected void basicReadExternal(int msgType, ObjectInput in) throws IOException {
    Assert.assertEquals(ZAP_NODE_REQUEST, msgType);
    zapNodeType = in.readInt();
    reason = in.readUTF();
  }

  protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
    Assert.assertEquals(ZAP_NODE_REQUEST, msgType);
    out.writeInt(zapNodeType);
    out.writeUTF(reason);
  }

  public String toString() {
    return "GroupZapNodeMessage [ " + zapNodeType + " , " + reason + " ]";
  }

  public String getReason() {
    return reason;
  }

  public int getZapNodeType() {
    return zapNodeType;
  }

}
