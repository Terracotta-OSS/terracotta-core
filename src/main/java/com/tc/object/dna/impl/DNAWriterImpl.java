/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCByteBufferOutputStream.Mark;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.util.Assert;
import com.tc.util.Conversion;

public class DNAWriterImpl implements DNAWriter {

  private static final int               UNINITIALIZED  = -1;

  private final TCByteBufferOutputStream output;
  private final Mark                     headerMark;
  private final ObjectStringSerializer   serializer;
  private final DNAEncodingInternal      encoding;

  private byte                           flags          = 0;
  private int                            firstLength    = UNINITIALIZED;
  private int                            totalLength    = UNINITIALIZED;
  private int                            lastStreamPos  = UNINITIALIZED;
  private int                            actionCount    = 0;

  public DNAWriterImpl(TCByteBufferOutputStream output, ObjectID id, String className,
                       ObjectStringSerializer serializer, DNAEncodingInternal encoding, boolean isDelta) {
    this(output, id, className, serializer, encoding, DNA.NULL_VERSION, isDelta);
  }

  protected DNAWriterImpl(TCByteBufferOutputStream output, ObjectID id, String className,
                          ObjectStringSerializer serializer, DNAEncodingInternal encoding, long version, boolean isDelta) {
    this.output = output;
    this.encoding = encoding;
    this.serializer = serializer;

    this.headerMark = output.mark();
    output.writeInt(UNINITIALIZED); // reserve 4 bytes for total length of this DNA
    output.writeInt(UNINITIALIZED); // reserve 4 bytes for # of actions

    output.writeByte(flags);
    output.writeLong(id.toLong());

    if (!isDelta) {
      serializer.writeString(output, className);
    }

    flags = Conversion.setFlag(flags, DNA.IS_DELTA, isDelta);

    if (version != DNA.NULL_VERSION) {
      flags = Conversion.setFlag(flags, DNA.HAS_VERSION, true);
      output.writeLong(version);
    }
  }

  @Override
  public void setIgnoreMissing(boolean ignoreMissing) {
    flags = Conversion.setFlag(flags, DNA.IGNORE_MISSING_OBJECT, ignoreMissing);
  }

  @Override
  public void markSectionEnd() {
    if (lastStreamPos != UNINITIALIZED) { throw new IllegalStateException("lastStreamPos=" + lastStreamPos); }
    if (totalLength != UNINITIALIZED) { throw new IllegalStateException("totalLength=" + totalLength); }
    lastStreamPos = output.getBytesWritten();
    firstLength = totalLength = output.getBytesWritten() - headerMark.getPosition();
  }

  @Override
  public void addLogicalAction(LogicalOperation method, Object[] parameters, LogicalChangeID logicalChangeID) {
    actionCount++;
    output.writeByte(BaseDNAEncodingImpl.LOGICAL_ACTION_TYPE);
    output.writeBoolean(logicalChangeID.isNull());
    if (!logicalChangeID.isNull()) {
      output.writeLong(logicalChangeID.toLong());
    }
    output.writeInt(method.ordinal()); // use a short instead?
    output.writeByte(parameters.length);

    for (Object parameter : parameters) {
      encoding.encode(parameter, output, serializer);
    }
  }

  @Override
  public void addSubArrayAction(int start, Object array, int length) {
    actionCount++;
    output.writeByte(BaseDNAEncodingImpl.SUB_ARRAY_ACTION_TYPE);
    output.writeInt(start);
    encoding.encodeArray(array, output, length);
  }

  /**
   * NOTE::This method is uses the value to decide if the field is actually a referencable fields (meaning it is a non
   * literal type.) This implementation is slightly flawed as you can set an instance of Integer or String to Object.
   * But since that can only happens in Physical applicator and it correctly calls the other interface, this is left
   * intact for now.
   */
  @Override
  public void addPhysicalAction(String fieldName, Object value) {
    addPhysicalAction(fieldName, value, value instanceof ObjectID);
  }

  /**
   * NOTE::README This method is called from instrumented code in the L2.
   * 
   * @see PhysicalStateClassLoader.createBasicDehydrateMethod()
   */
  @Override
  public void addPhysicalAction(String fieldName, Object value, boolean canBeReferenced) {
    if (value == null) {
      // Normally null values are converted into Null ObjectID much earlier, but this is not true when there are
      // multiple versions of a class in a cluster sharing data.
      value = ObjectID.NULL_ID;
      canBeReferenced = true;
    }

    actionCount++;
    if (canBeReferenced) {
      // An Object reference can be set to a literal instance, like
      // Object o = Integer.valueOf(10);
      // NOTE::Earlier we used to also check LiteralValues.isLiteralInstance(value) before entering this block, but I
      // think that is unnecessary and wrong when we optimize later to store ObjectIDs as longs in most cases in the L2
      output.writeByte(BaseDNAEncodingImpl.PHYSICAL_ACTION_TYPE_REF_OBJECT);
    } else {
      output.writeByte(BaseDNAEncodingImpl.PHYSICAL_ACTION_TYPE);
    }
    serializer.writeFieldName(output, fieldName);
    encoding.encode(value, output);
  }

  @Override
  public void addArrayElementAction(int index, Object value) {
    actionCount++;
    output.writeByte(BaseDNAEncodingImpl.ARRAY_ELEMENT_ACTION_TYPE);
    output.writeInt(index);
    encoding.encode(value, output);
  }

  @Override
  public void addEntireArray(Object value) {
    actionCount++;
    output.writeByte(BaseDNAEncodingImpl.ENTIRE_ARRAY_ACTION_TYPE);
    encoding.encodeArray(value, output);
  }

  @Override
  public void addLiteralValue(Object value) {
    actionCount++;
    output.writeByte(BaseDNAEncodingImpl.LITERAL_VALUE_ACTION_TYPE);
    encoding.encode(value, output);
  }


  @Override
  public void finalizeHeader() {
    if (Conversion.getFlag(flags, DNA.IS_DELTA) && actionCount == 0) {
      // this scenario (empty delta DNA) should be caught when txns are committed
      throw new AssertionError("sending delta DNA with no actions!");
    }

    byte[] lengths = new byte[9];
    Conversion.writeInt(totalLength, lengths, 0);
    Conversion.writeInt(actionCount, lengths, 4);

    lengths[8] = flags;
    this.headerMark.write(lengths);
  }

  @Override
  public void setArrayLength(int length) {
    checkVariableHeaderEmpty();
    flags = Conversion.setFlag(flags, DNA.HAS_ARRAY_LENGTH, true);
    output.writeInt(length);
  }

  private void checkVariableHeaderEmpty() {
    Assert.assertEquals(0, actionCount);
    Assert.assertFalse(Conversion.getFlag(flags, DNA.HAS_ARRAY_LENGTH));
  }

  @Override
  public int getActionCount() {
    return actionCount;
  }

  @Override
  public void copyTo(TCByteBufferOutput dest) {
    headerMark.copyTo(dest, firstLength);
  }

  @Override
  public void addLogicalAction(LogicalOperation method, Object[] parameters) {
    addLogicalAction(method, parameters, LogicalChangeID.NULL_ID);
  }

}
