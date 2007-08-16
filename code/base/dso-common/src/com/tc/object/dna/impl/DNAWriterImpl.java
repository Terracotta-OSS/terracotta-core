/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCByteBufferOutputStream.Mark;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.util.Conversion;

public class DNAWriterImpl implements DNAWriter {

  private static final long              NULL_ID       = ObjectID.NULL_ID.toLong();

  private final TCByteBufferOutputStream output;
  private final Mark                     headerMark;
  private final Mark                     parentIdMark;
  private final Mark                     arrayLengthMark;
  private final ObjectStringSerializer   serializer;
  private final DNAEncoding              encoding;
  private int                            actionCount   = 0;

  public DNAWriterImpl(TCByteBufferOutputStream output, ObjectID id, String className,
                       ObjectStringSerializer serializer, DNAEncoding encoding, String loaderDesc) {
    this.output = output;
    this.encoding = encoding;

    this.headerMark = output.mark();
    output.writeInt(-1); // reserve 4 bytes for total length of this DNA
    output.writeInt(-1); // reserve 4 bytes for # of actions
    output.writeBoolean(true);
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
    output.writeByte(DNAEncodingImpl.LOGICAL_ACTION_TYPE);
    output.writeInt(method);
    output.writeByte(parameters.length);

    for (int i = 0; i < parameters.length; i++) {
      encoding.encode(parameters[i], output);
    }
  }

  public void addSubArrayAction(int start, Object array, int length) {
    actionCount++;
    output.writeByte(DNAEncodingImpl.SUB_ARRAY_ACTION_TYPE);
    output.writeInt(start);
    encoding.encodeArray(array, output, length);
  }

  public void addClassLoaderAction(String classLoaderFieldName, Object value) {
    actionCount++;
    output.writeByte(DNAEncodingImpl.PHYSICAL_ACTION_TYPE);
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

  /**
   * NOTE::README:XXX: This method is called from instrumented code in the L2.
   * 
   * @see PhysicalStateClassLoader.createBasicDehydrateMethod()
   */
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
      // Object o = new Integer(10);
      // XXX::Earlier we used to also check LiteralValues.isLiteralInstance(value) before entering this block, but I
      // think that is unnecessary and wrong when we optimize later to store ObjectIDs as longs in most cases in the L2
      output.writeByte(DNAEncodingImpl.PHYSICAL_ACTION_TYPE_REF_OBJECT);
    } else {
      output.writeByte(DNAEncodingImpl.PHYSICAL_ACTION_TYPE);
    }
    serializer.writeFieldName(output, fieldName);
    encoding.encode(value, output);
  }

  public void addArrayElementAction(int index, Object value) {
    actionCount++;
    output.writeByte(DNAEncodingImpl.ARRAY_ELEMENT_ACTION_TYPE);
    output.writeInt(index);
    encoding.encode(value, output);
  }

  public void addEntireArray(Object value) {
    actionCount++;
    output.writeByte(DNAEncodingImpl.ENTIRE_ARRAY_ACTION_TYPE);
    encoding.encodeArray(value, output);
  }

  public void addLiteralValue(Object value) {
    actionCount++;
    output.writeByte(DNAEncodingImpl.LITERAL_VALUE_ACTION_TYPE);
    encoding.encode(value, output);
  }

  public void finalizeDNA(boolean isDelta) {
    int totalLength = this.output.getBytesWritten() - this.headerMark.getPosition();
    byte[] lengths = new byte[9];
    Conversion.writeInt(totalLength, lengths, 0);
    Conversion.writeInt(actionCount, lengths, 4);
    lengths[8] = isDelta ? (byte) 1 : (byte) 0;
    this.headerMark.write(lengths);
  }

  public void setParentObjectID(ObjectID id) {
    this.parentIdMark.write(Conversion.long2Bytes(id.toLong()));
  }

  public void setArrayLength(int length) {
    this.arrayLengthMark.write(Conversion.int2Bytes(length));
  }
}
