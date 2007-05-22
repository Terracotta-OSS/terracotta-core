/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet2;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

public class ObjectListSyncMessage extends AbstractGroupMessage {

  public static final int REQUEST  = 0;
  public static final int RESPONSE = 1;

  private Set             oids;

  // To make serialization happy
  public ObjectListSyncMessage() {
    super(-1);
  }

  public ObjectListSyncMessage(int type) {
    super(type);
  }

  public ObjectListSyncMessage(MessageID messageID, int type, Set oids) {
    super(type, messageID);
    this.oids = oids;
  }

  protected void basicReadExternal(int msgType, ObjectInput in) throws IOException {
    switch (msgType) {
      case REQUEST:
        // Nothing to read
        break;
      case RESPONSE:
        oids = readObjectIDS(in, new ObjectIDSet2());
        break;
      default:
        throw new AssertionError("Unknown Message Type : " + msgType);
    }
  }

  protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
    switch (msgType) {
      case REQUEST:
        // Nothing to write
        break;
      case RESPONSE:
        Assert.assertNotNull(oids);
        writeObjectIDS(out, oids);
        break;
      default:
        throw new AssertionError("Unknown Message Type : " + msgType);
    }
  }

  public Set getObjectIDs() {
    Assert.assertNotNull(oids);
    return oids;
  }

  public String toString() {
    return "ObjectListSyncMessage [ " + messageFrom() + ", type = " + getTypeString() + ", " + oids + "]";
  }

  private String getTypeString() {
    switch (getType()) {
      case REQUEST:
        return "REQUEST";
      case RESPONSE:
        return "RESPONSE";
      default:
        throw new AssertionError("Unknow Type ! : " + getType());
    }
  }

}
