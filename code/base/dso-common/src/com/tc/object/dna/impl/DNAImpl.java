/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.IDNAEncoding;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.util.Assert;

import java.io.IOException;

public class DNAImpl implements DNA, DNACursor, TCSerializable {
  private static final IDNAEncoding     DNA_STORAGE_ENCODING = new DNAEncoding(IDNAEncoding.STORAGE);

  private final ObjectStringSerializer serializer;
  private final boolean                createOutput;

  protected TCByteBufferInputStream    input;
  protected TCByteBuffer[]             dataOut;

  private int                          actionCount          = 0;
  private int                          origActionCount;
  private boolean                      isDelta;

  // Header info; parsed on deserializeFrom()
  private ObjectID                     id;
  private ObjectID                     parentID;
  private String                       typeName;
  private int                          arrayLength;
  private String                       loaderDesc;

  // XXX: cleanup type of this field
  private Object                       currentAction;

  private boolean                      wasDeserialized      = false;

  public DNAImpl(ObjectStringSerializer serializer, boolean createOutput) {
    this.serializer = serializer;
    this.createOutput = createOutput;
  }

  public String getTypeName() {
    return typeName;
  }

  public ObjectID getObjectID() throws DNAException {
    return id;
  }

  public ObjectID getParentObjectID() throws DNAException {
    return parentID;
  }

  public DNACursor getCursor() {
    return this;
  }

  public boolean next() throws IOException {
    try {
      return next(DNA_STORAGE_ENCODING);
    } catch (ClassNotFoundException e) {
      // This shouldn't happen when expand is "false"
      throw Assert.failure("Internal error");
    }
  }

  public boolean next(IDNAEncoding encoding) throws IOException, ClassNotFoundException {
    boolean hasNext = actionCount > 0;
    if (hasNext) {
      parseNext(encoding);
      actionCount--;
    } else {
      if (input.available() > 0) { throw new IOException(input.available() + " bytes remaining (expect 0)"); }
    }
    return hasNext;
  }

  private void parseNext(IDNAEncoding encoding) throws IOException, ClassNotFoundException {
    byte recordType = input.readByte();

    switch (recordType) {
      case DNAEncoding.PHYSICAL_ACTION_TYPE:
        parsePhysical(encoding, false);
        return;
      case DNAEncoding.PHYSICAL_ACTION_TYPE_REF_OBJECT:
        parsePhysical(encoding, true);
        return;
      case DNAEncoding.LOGICAL_ACTION_TYPE:
        parseLogical(encoding);
        return;
      case DNAEncoding.ARRAY_ELEMENT_ACTION_TYPE:
        parseArrayElement(encoding);
        return;
      case DNAEncoding.ENTIRE_ARRAY_ACTION_TYPE:
        parseEntireArray(encoding);
        return;
      case DNAEncoding.LITERAL_VALUE_ACTION_TYPE:
        parseLiteralValue(encoding);
        return;
      case DNAEncoding.SUB_ARRAY_ACTION_TYPE:
        parseSubArray(encoding);
        return;
      default:
        throw new IOException("Invalid record type: " + recordType);
    }

    // unreachable
  }

  private void parseSubArray(IDNAEncoding encoding) throws IOException, ClassNotFoundException {
    int startPos = input.readInt();
    Object subArray = encoding.decode(input);
    currentAction = new PhysicalAction(subArray, startPos);
  }

  private void parseEntireArray(IDNAEncoding encoding) throws IOException, ClassNotFoundException {
    Object array = encoding.decode(input);
    currentAction = new PhysicalAction(array);
  }

  private void parseLiteralValue(IDNAEncoding encoding) throws IOException, ClassNotFoundException {
    Object value = encoding.decode(input);
    currentAction = new LiteralAction(value);
  }

  private void parseArrayElement(IDNAEncoding encoding) throws IOException, ClassNotFoundException {
    int index = input.readInt();
    Object value = encoding.decode(input);
    currentAction = new PhysicalAction(index, value, value instanceof ObjectID);
  }

  private void parsePhysical(IDNAEncoding encoding, boolean isReference) throws IOException, ClassNotFoundException {
    String fieldName = serializer.readFieldName(input);

    Object value = encoding.decode(input);
    currentAction = new PhysicalAction(fieldName, value, value instanceof ObjectID || isReference);
  }

  private void parseLogical(IDNAEncoding encoding) throws IOException, ClassNotFoundException {
    int method = input.readInt();
    int paramCount = input.read();
    if (paramCount < 0) throw new AssertionError("Invalid param count:" + paramCount);
    Object[] params = new Object[paramCount];
    for (int i = 0; i < params.length; i++) {
      params[i] = encoding.decode(input);
    }
    currentAction = new LogicalAction(method, params);
  }

  public LogicalAction getLogicalAction() {
    return (LogicalAction) this.currentAction;
  }

  public PhysicalAction getPhysicalAction() {
    return (PhysicalAction) this.currentAction;
  }

  public Object getAction() {
    return this.currentAction;
  }

  public String toString() {
    try {
      StringBuffer buf = new StringBuffer();
      buf.append("DNAImpl\n");
      buf.append("{\n");
      buf.append("  type->" + getTypeName() + "\n");
      buf.append("  id->" + getObjectID() + "\n");
      buf.append("  version->" + getVersion() + "\n");
      buf.append("  isDelta->" + isDelta() + "\n");
      buf.append("  actionCount->" + actionCount + "\n");
      buf.append("  actionCount (orig)->" + origActionCount + "\n");
      buf.append("  deserialized?->" + wasDeserialized + "\n");
      buf.append("}\n");
      return buf.toString();
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  public int getArraySize() {
    return arrayLength;
  }

  public boolean hasLength() {
    return getArraySize() >= 0;
  }

  public long getVersion() {
    return NULL_VERSION;
  }

  /*
   * This methods is synchronized coz both broadcast stage and L2 sync objects stage accesses it simultaneously
   */
  public synchronized void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.write(dataOut);
  }

  public Object deserializeFrom(TCByteBufferInputStream serialInput) throws IOException {
    this.wasDeserialized = true;

    serialInput.mark();
    int dnaLength = serialInput.readInt();
    if (dnaLength <= 0) throw new IOException("Invalid length:" + dnaLength);

    serialInput.tcReset();

    this.input = serialInput.duplicateAndLimit(dnaLength);
    serialInput.skip(dnaLength);

    if (createOutput) {
      // this is optional (it's only needed on the server side for txn broadcasts)
      this.dataOut = input.toArray();
    }

    // skip over the length
    input.readInt();

    this.actionCount = input.readInt();
    this.origActionCount = actionCount;

    if (actionCount < 0) throw new IOException("Invalid action count:" + actionCount);

    this.isDelta = input.readBoolean();

    this.id = new ObjectID(input.readLong());
    this.parentID = new ObjectID(input.readLong());
    this.typeName = serializer.readString(input);
    this.loaderDesc = serializer.readString(input);
    this.arrayLength = input.readInt();

    return this;
  }

  public String getDefiningLoaderDescription() {
    return this.loaderDesc;
  }

  public int getActionCount() {
    return this.actionCount;
  }

  public boolean isDelta() {
    return this.isDelta;
  }

  public void reset() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Reset is not supported by this class");
  }

}
