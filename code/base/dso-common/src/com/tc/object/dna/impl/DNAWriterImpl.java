/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCByteBufferOutputStream.Mark;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNAWriter;
import com.tc.util.Conversion;

public class DNAWriterImpl implements DNAWriter {

  private static final long              NULL_ID       = ObjectID.NULL_ID.toLong();
  private static final LiteralValues     literalValues = new LiteralValues();

  private final TCByteBufferOutputStream output;
  private final Mark                     headerMark;
  private final Mark                     parentIdMark;
  private final Mark                     arrayLengthMark;
  private final ObjectStringSerializer   serializer;
  private final DNAEncoding              encoding;
  private int                            actionCount   = 0;

  public DNAWriterImpl(TCByteBufferOutputStream output, ObjectID id, String className,
                       ObjectStringSerializer serializer, DNAEncoding encoding, String loaderDesc, boolean isDelta) {
    this.output = output;
    this.encoding = encoding;

    this.headerMark = output.mark();
    output.writeInt(-1); // reserve 4 bytes for total length of this DNA
    output.writeInt(-1); // reserve 4 bytes for # of actions
    output.writeBoolean(isDelta);
    output.writeLong(id.toLong());
    this.parentIdMark = output.mark();
    output.writeLong(NULL_ID); // reserve 8 bytes for the parent object ID
    this.serializer = serializer;
    serializer.writeString(output, className);
    serializer.writeString(output, loaderDesc);
    this.arrayLengthMark = output.mark();
    output.writeInt(-1); // reserve 4 bytes for array length
  }

  public void addLogicalAction(int method, Object[] parameters) {
    actionCount++;
    output.writeByte(DNAEncoding.LOGICAL_ACTION_TYPE);
    output.writeInt(method);
    output.writeByte(parameters.length);

    for (int i = 0; i < parameters.length; i++) {
      encoding.encode(parameters[i], output);
    }
  }

  public void addSubArrayAction(int start, Object array, int length) {
    actionCount++;
    output.writeByte(DNAEncoding.SUB_ARRAY_ACTION_TYPE);
    output.writeInt(start);
    encoding.encodeArray(array, output, length);
  }

  public void addClassLoaderAction(String classLoaderFieldName, Object value) {
    actionCount++;
    output.writeByte(DNAEncoding.PHYSICAL_ACTION_TYPE);
    serializer.writeFieldName(output, classLoaderFieldName);
    encoding.encodeClassLoader(value, output);
  }

  /**
   * XXX::This method is uses the value to decide if the field is actually a referencable fields (meaning it is a non
   * literal type.) This implementation is slightly flawed as you can set an instance of Integer or String to Object.
   * But since that can only happens in Physical applicator and it correctly calls the other interface, this is left
   * intact for now.
   */
  public void addPhysicalAction(String fieldName, Object value) {
    addPhysicalAction(fieldName, value, value instanceof ObjectID);
  }

  public void addPhysicalAction(String fieldName, Object value, boolean canBeReference) {
    actionCount++;
    if (canBeReference && literalValues.isLiteralInstance(value)) {
      // an Object reference is set to a literal instance
      output.writeByte(DNAEncoding.PHYSICAL_ACTION_TYPE_REF_OBJECT);
    } else {
      output.writeByte(DNAEncoding.PHYSICAL_ACTION_TYPE);
    }
    serializer.writeFieldName(output, fieldName);
    encoding.encode(value, output);
  }

  public void addArrayElementAction(int index, Object value) {
    actionCount++;
    output.writeByte(DNAEncoding.ARRAY_ELEMENT_ACTION_TYPE);
    output.writeInt(index);
    encoding.encode(value, output);
  }

  public void addEntireArray(Object value) {
    actionCount++;
    output.writeByte(DNAEncoding.ENTIRE_ARRAY_ACTION_TYPE);
    encoding.encodeArray(value, output);
  }

  public void addLiteralValue(Object value) {
    actionCount++;
    output.writeByte(DNAEncoding.LITERAL_VALUE_ACTION_TYPE);
    encoding.encode(value, output);
  }

  private void finalizeHeader() {
    int totalLength = this.output.getBytesWritten() - this.headerMark.getPosition();
    byte[] lengths = new byte[8];
    Conversion.writeInt(totalLength, lengths, 0);
    Conversion.writeInt(actionCount, lengths, 4);
    this.headerMark.write(lengths);
  }

  public void finalizeDNA() {
    finalizeHeader();
  }

  public void setParentObjectID(ObjectID id) {
    this.parentIdMark.write(Conversion.long2Bytes(id.toLong()));
  }

  public void setArrayLength(int length) {
    this.arrayLengthMark.write(Conversion.int2Bytes(length));
  }
}
