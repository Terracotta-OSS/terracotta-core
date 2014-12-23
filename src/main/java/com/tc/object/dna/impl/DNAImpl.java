/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferInput.Mark;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.util.Assert;
import com.tc.util.Conversion;

import java.io.IOException;

public class DNAImpl implements DNA, DNACursor, TCSerializable {
  private static final DNAEncodingInternal DNA_STORAGE_ENCODING  = new StorageDNAEncodingImpl();

  private final ObjectStringSerializer     serializer;
  private final boolean                    createOutput;

  protected TCByteBufferInput              input;
  protected TCByteBuffer[]                 dataOut;

  private int                              actionCount           = 0;
  private int                              origActionCount;
  private boolean                          isDelta;

  // Header info; parsed on deserializeFrom()
  private ObjectID                         id;
  private ObjectID                         parentID;
  private String                           typeName;
  private int                              arrayLength;
  private long                             version;
  private int                              dnaLength;

  // XXX: cleanup type of this field
  private Object                           currentAction;

  private boolean                          wasDeserialized       = false;

  public DNAImpl(final ObjectStringSerializer serializer, final boolean createOutput) {
    this.serializer = serializer;
    this.createOutput = createOutput;
  }

  @Override
  public String getTypeName() {
    return this.typeName;
  }

  // This method is there for debugging/logging stats. Should never be used otherwise.
  public void setTypeClassName(final String className) {
    if (this.typeName == null) {
      this.typeName = className;
    }
  }

  @Override
  public ObjectID getObjectID() throws DNAException {
    return this.id;
  }

  @Override
  public ObjectID getParentObjectID() throws DNAException {
    return this.parentID;
  }

  @Override
  public DNACursor getCursor() {
    return this;
  }

  @Override
  public boolean next() throws IOException {
    try {
      return next(DNA_STORAGE_ENCODING);
    } catch (final ClassNotFoundException e) {
      // This shouldn't happen when expand is "false"
      throw Assert.failure("Internal error");
    }
  }

  @Override
  public boolean next(final DNAEncoding encoding) throws IOException, ClassNotFoundException {
    // yucky cast
    DNAEncodingInternal encodingInternal = (DNAEncodingInternal) encoding;

    final boolean hasNext = this.actionCount > 0;
    if (hasNext) {
      parseNext(encodingInternal);
      this.actionCount--;
    } else {
        if (this.input.available() != 0) {
          throw new IOException(this.input.available() + " bytes remaining (expect " + 0 + ")");
        }
      }
    return hasNext;
  }

  private void parseNext(final DNAEncodingInternal encoding) throws IOException, ClassNotFoundException {
    final byte recordType = this.input.readByte();

    switch (recordType) {
      case BaseDNAEncodingImpl.PHYSICAL_ACTION_TYPE:
        parsePhysical(encoding, false);
        return;
      case BaseDNAEncodingImpl.PHYSICAL_ACTION_TYPE_REF_OBJECT:
        parsePhysical(encoding, true);
        return;
      case BaseDNAEncodingImpl.LOGICAL_ACTION_TYPE:
        parseLogical(encoding);
        return;
      case BaseDNAEncodingImpl.ARRAY_ELEMENT_ACTION_TYPE:
        parseArrayElement(encoding);
        return;
      case BaseDNAEncodingImpl.ENTIRE_ARRAY_ACTION_TYPE:
        parseEntireArray(encoding);
        return;
      case BaseDNAEncodingImpl.LITERAL_VALUE_ACTION_TYPE:
        parseLiteralValue(encoding);
        return;
      case BaseDNAEncodingImpl.SUB_ARRAY_ACTION_TYPE:
        parseSubArray(encoding);
        return;
      default:
        throw new IOException("Invalid record type: " + recordType);
    }

    // unreachable
  }

  private void parseSubArray(final DNAEncodingInternal encoding) throws IOException, ClassNotFoundException {
    final int startPos = this.input.readInt();
    final Object subArray = encoding.decode(this.input);
    this.currentAction = new PhysicalAction(subArray, startPos);
  }

  private void parseEntireArray(final DNAEncodingInternal encoding) throws IOException, ClassNotFoundException {
    final Object array = encoding.decode(this.input);
    this.currentAction = new PhysicalAction(array);
  }

  private void parseLiteralValue(final DNAEncodingInternal encoding) throws IOException, ClassNotFoundException {
    final Object value = encoding.decode(this.input);
    this.currentAction = new LiteralAction(value);
  }

  private void parseArrayElement(final DNAEncodingInternal encoding) throws IOException, ClassNotFoundException {
    final int index = this.input.readInt();
    final Object value = encoding.decode(this.input);
    this.currentAction = new PhysicalAction(index, value, value instanceof ObjectID);
  }

  private void parsePhysical(final DNAEncodingInternal encoding, final boolean isReference) throws IOException,
      ClassNotFoundException {
    final String fieldName = this.serializer.readFieldName(this.input);

    final Object value = encoding.decode(this.input);
    this.currentAction = new PhysicalAction(fieldName, value, value instanceof ObjectID || isReference);
  }

  private void parseLogical(final DNAEncodingInternal encoding) throws IOException, ClassNotFoundException {
    LogicalChangeID logicalChangeID = LogicalChangeID.NULL_ID;
    if (!input.readBoolean()) {
      logicalChangeID = new LogicalChangeID(input.readLong());
    }
    final LogicalOperation method = LogicalOperation.values()[this.input.readInt()];
    final int paramCount = this.input.read();
    if (paramCount < 0) { throw new AssertionError("Invalid param count:" + paramCount); }
    final Object[] params = new Object[paramCount];
    for (int i = 0; i < params.length; i++) {
      params[i] = encoding.decode(this.input, serializer);
    }
    this.currentAction = new LogicalAction(method, params, logicalChangeID);
  }

  @Override
  public LogicalAction getLogicalAction() {
    return (LogicalAction) this.currentAction;
  }

  @Override
  public PhysicalAction getPhysicalAction() {
    return (PhysicalAction) this.currentAction;
  }

  @Override
  public Object getAction() {
    return this.currentAction;
  }

  @Override
  public String toString() {
    try {
      final StringBuffer buf = new StringBuffer();
      buf.append("DNAImpl\n");
      buf.append("{\n");
      buf.append("  type->" + getTypeName() + "\n");
      buf.append("  id->" + getObjectID() + "\n");
      buf.append("  version->" + getVersion() + "\n");
      buf.append("  isDelta->" + isDelta() + "\n");
      buf.append("  actionCount->" + this.actionCount + "\n");
      buf.append("  actionCount (orig)->" + this.origActionCount + "\n");
      buf.append("  deserialized?->" + this.wasDeserialized + "\n");
      buf.append("}\n");
      return buf.toString();
    } catch (final Exception e) {
      return e.getMessage();
    }
  }

  @Override
  public int getArraySize() {
    return this.arrayLength;
  }

  @Override
  public boolean hasLength() {
    return getArraySize() >= 0;
  }

  @Override
  public long getVersion() {
    return this.version;
  }

  /*
   * This methods is synchronized coz both broadcast stage and L2 sync objects stage accesses it simultaneously
   */
  @Override
  public synchronized void serializeTo(final TCByteBufferOutput serialOutput) {
    serialOutput.write(this.dataOut);
  }

  @Override
  public Object deserializeFrom(final TCByteBufferInput serialInput) throws IOException {
    this.wasDeserialized = true;

    final Mark mark = serialInput.mark();
    dnaLength = serialInput.readInt();
    if (dnaLength <= 0) { throw new IOException("Invalid length:" + dnaLength); }

    serialInput.tcReset(mark);

    this.input = serialInput.duplicateAndLimit(dnaLength);
    serialInput.skip(dnaLength);

    if (this.createOutput) {
      // this is optional (it's only needed on the server side for txn broadcasts)
      this.dataOut = this.input.toArray();
    }

    // skip over the length
    this.input.readInt();

    this.actionCount = this.input.readInt();
    this.origActionCount = this.actionCount;

    if (this.actionCount < 0) { throw new IOException("Invalid action count:" + this.actionCount); }

    final byte flags = this.input.readByte();

    this.id = new ObjectID(this.input.readLong());

    this.isDelta = Conversion.getFlag(flags, DNA.IS_DELTA);

    if (!this.isDelta) {
      this.typeName = this.serializer.readString(this.input);
    }

    if (Conversion.getFlag(flags, DNA.HAS_VERSION)) {
      this.version = this.input.readLong();
    } else {
      this.version = DNA.NULL_VERSION;
    }

    if (Conversion.getFlag(flags, DNA.HAS_PARENT_ID)) {
      this.parentID = new ObjectID(this.input.readLong());
    } else {
      this.parentID = ObjectID.NULL_ID;
    }

    if (Conversion.getFlag(flags, DNA.HAS_ARRAY_LENGTH)) {
      this.arrayLength = this.input.readInt();
    } else {
      this.arrayLength = DNA.NULL_ARRAY_SIZE;
    }

    return this;
  }

  @Override
  public int getActionCount() {
    return this.actionCount;
  }

  @Override
  public boolean isDelta() {
    return this.isDelta;
  }

  @Override
  public void reset() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Reset is not supported by this class");
  }

}
