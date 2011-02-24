/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferInput.Mark;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAInternal;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.metadata.MetaDataDescriptorImpl;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.util.Assert;
import com.tc.util.Conversion;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

public class DNAImpl implements DNAInternal, DNACursor, TCSerializable {
  private static final DNAEncodingInternal DNA_STORAGE_ENCODING  = new StorageDNAEncodingImpl();
  public static final MetaDataReader       NULL_META_DATA_READER = new NullMetaDataReader();

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
  private String                           loaderDesc;
  private long                             version;
  private int                              dnaLength;
  private int                              metaDataOffset;

  // XXX: cleanup type of this field
  private Object                           currentAction;

  private boolean                          wasDeserialized       = false;

  private MetaDataReader                   metaDataReader        = NULL_META_DATA_READER;

  public DNAImpl(final ObjectStringSerializer serializer, final boolean createOutput) {
    this.serializer = serializer;
    this.createOutput = createOutput;
  }

  public String getTypeName() {
    return this.typeName;
  }

  // This method is there for debugging/logging stats. Should never be used otherwise.
  public void setTypeClassName(final String className) {
    if (this.typeName == null) {
      this.typeName = className;
    }
  }

  public ObjectID getObjectID() throws DNAException {
    return this.id;
  }

  public ObjectID getParentObjectID() throws DNAException {
    return this.parentID;
  }

  public DNACursor getCursor() {
    return this;
  }

  public MetaDataReader getMetaDataReader() {
    return metaDataReader;
  }

  public boolean hasMetaData() {
    return metaDataOffset > 0;
  }

  public boolean next() throws IOException {
    try {
      return next(DNA_STORAGE_ENCODING);
    } catch (final ClassNotFoundException e) {
      // This shouldn't happen when expand is "false"
      throw Assert.failure("Internal error");
    }
  }

  public boolean next(final DNAEncoding encoding) throws IOException, ClassNotFoundException {
    // yucky cast
    DNAEncodingInternal encodingInternal = (DNAEncodingInternal) encoding;

    final boolean hasNext = this.actionCount > 0;
    if (hasNext) {
      parseNext(encodingInternal);
      this.actionCount--;
    } else {
      int expect = 0;
      if (metaDataOffset > 0) {
        expect = dnaLength - metaDataOffset;
      }

      if (this.input.available() != expect) {
        //
        throw new IOException(this.input.available() + " bytes remaining (expect " + expect + ")");
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
    final int method = this.input.readInt();
    final int paramCount = this.input.read();
    if (paramCount < 0) { throw new AssertionError("Invalid param count:" + paramCount); }
    final Object[] params = new Object[paramCount];
    for (int i = 0; i < params.length; i++) {
      params[i] = encoding.decode(this.input, serializer);
    }
    this.currentAction = new LogicalAction(method, params);
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

  public int getArraySize() {
    return this.arrayLength;
  }

  public boolean hasLength() {
    return getArraySize() >= 0;
  }

  public long getVersion() {
    return this.version;
  }

  /*
   * This methods is synchronized coz both broadcast stage and L2 sync objects stage accesses it simultaneously
   */
  public synchronized void serializeTo(final TCByteBufferOutput serialOutput) {
    serialOutput.write(this.dataOut);
  }

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

    // read meta data offset
    metaDataOffset = this.input.readInt();

    final byte flags = this.input.readByte();

    this.id = new ObjectID(this.input.readLong());

    this.isDelta = Conversion.getFlag(flags, DNA.IS_DELTA);

    if (!this.isDelta) {
      this.typeName = this.serializer.readString(this.input);
      this.loaderDesc = this.serializer.readString(this.input);
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

    if (hasMetaData()) {
      TCByteBufferInput metaDataInput = this.input.duplicate();
      metaDataInput.skip(metaDataOffset - (this.input.getTotalLength() - this.input.available()));
      this.metaDataReader = new MetaDataReaderImpl(metaDataInput, serializer);
    }

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

  private static class MetaDataReaderImpl implements MetaDataReader {
    private final TCByteBufferInput      input;
    private final ObjectStringSerializer serializer;

    MetaDataReaderImpl(TCByteBufferInput input, ObjectStringSerializer serializer) {
      this.input = input;
      this.serializer = serializer;
    }

    public Iterator<MetaDataDescriptorInternal> iterator() {
      return new MetaDataIterator(input, serializer);
    }
  }

  private static class MetaDataIterator implements Iterator<MetaDataDescriptorInternal> {

    private final TCByteBufferInput      input;
    private final ObjectStringSerializer serializer;

    MetaDataIterator(TCByteBufferInput input, ObjectStringSerializer serializer) {
      this.input = input;
      this.serializer = serializer;
    }

    public boolean hasNext() {
      return input.available() > 0;
    }

    public MetaDataDescriptorInternal next() {
      try {
        int length = input.readInt();

        Mark start = input.mark();
        input.skip(length - 4); // length includes the "length" int (thus -4)
        Mark end = input.mark();

        return MetaDataDescriptorImpl.deserializeInstance(new TCByteBufferInputStream(input.toArray(start, end)),
                                                          serializer);
      } catch (IOException e) {
        // XXX: don't like this runtime exception
        throw new RuntimeException(e);
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private static class NullMetaDataReader implements MetaDataReader {

    public Iterator<MetaDataDescriptorInternal> iterator() {
      return Collections.EMPTY_LIST.iterator();
    }

  }

}
