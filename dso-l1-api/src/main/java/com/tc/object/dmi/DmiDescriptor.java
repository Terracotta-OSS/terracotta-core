/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dmi;

import com.tc.async.api.EventContext;
import com.tc.io.TCByteBufferInput;
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

  /**
   * New empty descriptor
   */
  public DmiDescriptor() {
    receiverId = null;
    dmiCallId = null;
  }

  /**
   * New descriptor
   * 
   * @param receiverId Receiver object identifier
   * @param dmiCallId DMI call identifier
   * @param classSpecs Classes
   * @param faultReceiver True if should use local fault receiver
   */
  public DmiDescriptor(ObjectID receiverId, ObjectID dmiCallId, DmiClassSpec[] classSpecs, boolean faultReceiver) {
    Assert.pre(receiverId != null);
    Assert.pre(dmiCallId != null);
    Assert.pre(classSpecs != null);

    this.receiverId = receiverId;
    this.dmiCallId = dmiCallId;
    this.classSpecs = classSpecs;
    this.faultReceiver = faultReceiver;
  }

  /**
   * @return Receiver ID
   */
  public ObjectID getReceiverId() {
    return receiverId;
  }

  /**
   * @return DMI call ID
   */
  public ObjectID getDmiCallId() {
    return dmiCallId;
  }

  /**
   * @return Classes
   */
  public DmiClassSpec[] getClassSpecs() {
    return classSpecs;
  }

  /**
   * @return True if should use fault receiver
   */
  public boolean isFaultReceiver() {
    return faultReceiver;
  }

  @Override
  public String toString() {
    return "DmiDescriptor{receiverId=" + receiverId + ", dmiCallId=" + dmiCallId + ", ClassSpecs="
           + DmiClassSpec.toString(classSpecs) + "}";
  }

  /**
   * Deserialize descriptor from input stream into this object
   * 
   * @param in Input stream
   * @return this
   */
  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    receiverId = new ObjectID(in.readLong());
    dmiCallId = new ObjectID(in.readLong());
    faultReceiver = in.readBoolean();
    final int size = in.readInt();
    classSpecs = new DmiClassSpec[size];
    for (int i = 0; i < classSpecs.length; i++) {
      final String className = in.readString();
      classSpecs[i] = new DmiClassSpec(className);
    }
    return this;
  }

  /**
   * Serialize this descriptor to out
   * 
   * @param out Output stream
   */
  public void serializeTo(TCByteBufferOutput out) {
    out.writeLong(receiverId.toLong());
    out.writeLong(dmiCallId.toLong());
    out.writeBoolean(faultReceiver);
    out.writeInt(classSpecs.length);
    for (DmiClassSpec classSpec : classSpecs) {
      out.writeString(classSpec.getClassName());
    }
  }
}
