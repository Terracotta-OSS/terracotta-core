/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dmi;

import com.tc.async.api.EventContext;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.ObjectID;
import com.tc.util.Assert;

import java.io.IOException;

/**
 * Representation of a distributed method invocation
 */
public class DmiDescriptor implements TCSerializable, EventContext {

  public static final DmiDescriptor[] EMPTY_ARRAY = new DmiDescriptor[0];

  private ObjectID                    receiverId;
  private ObjectID                    dmiCallId;
  private DmiClassSpec[]              classSpecs;
  private boolean                     faultReceiver;

  public DmiDescriptor() {
    receiverId = null;
    dmiCallId = null;
  }

  public DmiDescriptor(ObjectID receiverId, ObjectID dmiCallId, DmiClassSpec[] classSpecs, boolean faultReceiver) {
    Assert.pre(receiverId != null);
    Assert.pre(dmiCallId != null);
    Assert.pre(classSpecs != null);

    this.receiverId = receiverId;
    this.dmiCallId = dmiCallId;
    this.classSpecs = classSpecs;
    this.faultReceiver = faultReceiver;
  }

  public ObjectID getReceiverId() {
    return receiverId;
  }

  public ObjectID getDmiCallId() {
    return dmiCallId;
  }

  public DmiClassSpec[] getClassSpecs() {
    return classSpecs;
  }

  public boolean isFaultReceiver() {
    return faultReceiver;
  }

  public String toString() {
    return "DmiDescriptor{receiverId=" + receiverId + ", dmiCallId=" + dmiCallId + ", ClassSpecs="
           + DmiClassSpec.toString(classSpecs) + "}";
  }

  public Object deserializeFrom(TCByteBufferInputStream in) throws IOException {
    receiverId = new ObjectID(in.readLong());
    dmiCallId = new ObjectID(in.readLong());
    faultReceiver = in.readBoolean();
    final int size = in.readInt();
    classSpecs = new DmiClassSpec[size];
    for (int i = 0; i < classSpecs.length; i++) {
      final String classLoaderDesc = in.readString();
      final String className = in.readString();
      classSpecs[i] = new DmiClassSpec(classLoaderDesc, className);
    }
    return this;
  }

  public void serializeTo(TCByteBufferOutput out) {
    out.writeLong(receiverId.toLong());
    out.writeLong(dmiCallId.toLong());
    out.writeBoolean(faultReceiver);
    out.writeInt(classSpecs.length);
    for (int i = 0; i < classSpecs.length; i++) {
      out.writeString(classSpecs[i].getClassLoaderDesc());
      out.writeString(classSpecs[i].getClassName());
    }
  }
}
