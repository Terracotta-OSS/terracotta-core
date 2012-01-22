/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCByteBufferOutputStream.Mark;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.DNAWriterInternal;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.util.Assert;
import com.tc.util.Conversion;

import java.nio.channels.IllegalSelectorException;
import java.util.ArrayList;
import java.util.List;

public class DNAWriterImpl implements DNAWriterInternal {

  private static final int               UNINITIALIZED  = -1;

  private final TCByteBufferOutputStream output;
  private final Mark                     headerMark;
  private final ObjectStringSerializer   serializer;
  private final DNAEncodingInternal      encoding;
  private final List<Appender>           appenders      = new ArrayList<Appender>(5);

  private byte                           flags          = 0;
  private int                            metaDataLength = UNINITIALIZED;
  private int                            firstLength    = UNINITIALIZED;
  private int                            totalLength    = UNINITIALIZED;
  private int                            lastStreamPos  = UNINITIALIZED;
  private int                            actionCount    = 0;
  private boolean                        contiguous     = true;
  private boolean                        hasMetaData    = false;
  private int                            metaDataOffset = UNINITIALIZED;

  public DNAWriterImpl(TCByteBufferOutputStream output, ObjectID id, String className,
                       ObjectStringSerializer serializer, DNAEncodingInternal encoding, String loaderDesc,
                       boolean isDelta) {
    this(output, id, className, serializer, encoding, loaderDesc, DNA.NULL_VERSION, isDelta);
  }

  protected DNAWriterImpl(TCByteBufferOutputStream output, ObjectID id, String className,
                          ObjectStringSerializer serializer, DNAEncodingInternal encoding, String loaderDesc,
                          long version, boolean isDelta) {
    this.output = output;
    this.encoding = encoding;
    this.serializer = serializer;

    this.headerMark = output.mark();
    output.writeInt(UNINITIALIZED); // reserve 4 bytes for total length of this DNA
    output.writeInt(UNINITIALIZED); // reserve 4 bytes for # of actions
    output.writeInt(UNINITIALIZED); // reserve 4 bytes for offset of meta data
    output.writeByte(flags);
    output.writeLong(id.toLong());

    if (!isDelta) {
      serializer.writeString(output, className);
      serializer.writeString(output, loaderDesc);
    }

    flags = Conversion.setFlag(flags, DNA.IS_DELTA, isDelta);

    if (version != DNA.NULL_VERSION) {
      flags = Conversion.setFlag(flags, DNA.HAS_VERSION, true);
      output.writeLong(version);
    }
  }

  public DNAWriter createAppender() {
    if (contiguous) {
      contiguous = !hasMetaData && (output.getBytesWritten() == lastStreamPos);
    }
    Appender appender = new Appender(this, output);
    appenders.add(appender);
    return appender;
  }

  public boolean isContiguous() {
    return contiguous;
  }

  public void markSectionEnd() {
    if (lastStreamPos != UNINITIALIZED) { throw new IllegalStateException("lastStreamPos=" + lastStreamPos); }
    if (totalLength != UNINITIALIZED) { throw new IllegalStateException("totalLength=" + totalLength); }
    lastStreamPos = output.getBytesWritten();
    firstLength = totalLength = output.getBytesWritten() - headerMark.getPosition();
  }

  private void appenderSectionEnd(int appenderLength) {
    if (contiguous) {
      lastStreamPos = output.getBytesWritten();
    }
    totalLength += appenderLength;
  }

  public void addLogicalAction(int method, Object[] parameters) {
    actionCount++;
    output.writeByte(BaseDNAEncodingImpl.LOGICAL_ACTION_TYPE);
    output.writeInt(method); // use a short instead?
    output.writeByte(parameters.length);

    for (Object parameter : parameters) {
      encoding.encode(parameter, output, serializer);
    }
  }

  public void addSubArrayAction(int start, Object array, int length) {
    actionCount++;
    output.writeByte(BaseDNAEncodingImpl.SUB_ARRAY_ACTION_TYPE);
    output.writeInt(start);
    encoding.encodeArray(array, output, length);
  }

  public void addClassLoaderAction(String classLoaderFieldName, ClassLoader value) {
    actionCount++;
    output.writeByte(BaseDNAEncodingImpl.PHYSICAL_ACTION_TYPE);
    serializer.writeFieldName(output, classLoaderFieldName);
    encoding.encodeClassLoader(value, output);
  }

  /**
   * NOTE::This method is uses the value to decide if the field is actually a referencable fields (meaning it is a non
   * literal type.) This implementation is slightly flawed as you can set an instance of Integer or String to Object.
   * But since that can only happens in Physical applicator and it correctly calls the other interface, this is left
   * intact for now.
   */
  public void addPhysicalAction(String fieldName, Object value) {
    addPhysicalAction(fieldName, value, value instanceof ObjectID);
  }

  /**
   * NOTE::README This method is called from instrumented code in the L2.
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

  public void addArrayElementAction(int index, Object value) {
    actionCount++;
    output.writeByte(BaseDNAEncodingImpl.ARRAY_ELEMENT_ACTION_TYPE);
    output.writeInt(index);
    encoding.encode(value, output);
  }

  public void addEntireArray(Object value) {
    actionCount++;
    output.writeByte(BaseDNAEncodingImpl.ENTIRE_ARRAY_ACTION_TYPE);
    encoding.encodeArray(value, output);
  }

  public void addLiteralValue(Object value) {
    actionCount++;
    output.writeByte(BaseDNAEncodingImpl.LITERAL_VALUE_ACTION_TYPE);
    encoding.encode(value, output);
  }

  public void addMetaData(MetaDataDescriptorInternal md) {
    addMetaData(md, false);
  }

  protected void addMetaData(MetaDataDescriptorInternal md, boolean fromAppender) {
    if (!hasMetaData) {
      hasMetaData = true;

      if (!fromAppender) {
        metaDataOffset = output.getBytesWritten() - headerMark.getPosition();
      }
    }

    Mark lengthMark = output.mark();
    output.writeInt(-1);
    md.serializeTo(output, serializer);

    int length = output.getBytesWritten() - lengthMark.getPosition();
    lengthMark.write(Conversion.int2Bytes(length));

    if (!fromAppender) {
      if (metaDataLength == UNINITIALIZED) {
        metaDataLength = length;
      } else {
        metaDataLength += length;
      }
    }
  }

  public void finalizeHeader() {
    if (Conversion.getFlag(flags, DNA.IS_DELTA) && actionCount == 0) {
      // this scenario (empty delta DNA) should be caught when txns are committed
      throw new AssertionError("sending delta DNA with no actions!");
    }

    byte[] lengths = new byte[13];
    Conversion.writeInt(totalLength, lengths, 0);
    Conversion.writeInt(actionCount, lengths, 4);

    int totalMetaDataLength = 0;
    if (hasMetaData && (metaDataLength != UNINITIALIZED)) {
      totalMetaDataLength = metaDataLength;
    }
    for (Appender a : appenders) {
      if (a.metaDataOffset != UNINITIALIZED) {
        totalMetaDataLength += (a.appendSectionLength - a.metaDataOffset);
      }
    }

    if (totalMetaDataLength != 0) {
      Conversion.writeInt(totalLength - totalMetaDataLength, lengths, 8);
    }

    lengths[12] = flags;
    this.headerMark.write(lengths);
  }

  public void setParentObjectID(ObjectID id) {
    checkVariableHeaderEmpty();
    flags = Conversion.setFlag(flags, DNA.HAS_PARENT_ID, true);
    output.writeLong(id.toLong());
  }

  public void setArrayLength(int length) {
    checkVariableHeaderEmpty();
    flags = Conversion.setFlag(flags, DNA.HAS_ARRAY_LENGTH, true);
    output.writeInt(length);
  }

  private void checkVariableHeaderEmpty() {
    Assert.assertEquals(0, actionCount);
    Assert.assertFalse(Conversion.getFlag(flags, DNA.HAS_PARENT_ID));
    Assert.assertFalse(Conversion.getFlag(flags, DNA.HAS_ARRAY_LENGTH));
  }

  public int getActionCount() {
    return actionCount;
  }

  public void copyTo(TCByteBufferOutput dest) {
    if (hasMetaData) {
      headerMark.copyTo(dest, 0, metaDataOffset == UNINITIALIZED ? firstLength : metaDataOffset);

      // copy all appender actions
      for (Appender appender : appenders) {
        appender.copyActionsTo(dest);
      }

      // copy this metadata (if present)
      if (this.metaDataLength != UNINITIALIZED) {
        headerMark.copyTo(dest, metaDataOffset, metaDataLength);
      }

      // copy all appender metadata
      for (Appender appender : appenders) {
        appender.copyMetaDataTo(dest);
      }
    } else {
      headerMark.copyTo(dest, firstLength);

      for (Appender appender : appenders) {
        appender.copyTo(dest);
      }
    }
  }

  private static class Appender implements DNAWriterInternal {
    private final DNAWriterImpl            parent;
    private final TCByteBufferOutputStream output;
    private final Mark                     startMark;
    private int                            appendSectionLength = UNINITIALIZED;
    private int                            metaDataOffset      = UNINITIALIZED;

    Appender(DNAWriterImpl parent, TCByteBufferOutputStream output) {
      this.parent = parent;
      this.output = output;
      this.startMark = output.mark();
    }

    void copyMetaDataTo(TCByteBufferOutput dest) {
      if (metaDataOffset != UNINITIALIZED) {
        startMark.copyTo(dest, metaDataOffset, metaDataLength());
      }
    }

    void copyActionsTo(TCByteBufferOutput dest) {
      int length = appendSectionLength - metaDataLength();
      startMark.copyTo(dest, 0, length);
    }

    private int metaDataLength() {
      if (appendSectionLength == UNINITIALIZED) { throw new IllegalSelectorException(); }

      if (metaDataOffset == UNINITIALIZED) { return 0; }

      return appendSectionLength - metaDataOffset;
    }

    public void addArrayElementAction(int index, Object value) {
      parent.addArrayElementAction(index, value);
    }

    public void addClassLoaderAction(String classLoaderFieldName, ClassLoader value) {
      parent.addClassLoaderAction(classLoaderFieldName, value);
    }

    public void addEntireArray(Object value) {
      parent.addEntireArray(value);
    }

    public void addLiteralValue(Object value) {
      parent.addLiteralValue(value);
    }

    public void addLogicalAction(int method, Object[] parameters) {
      parent.addLogicalAction(method, parameters);
    }

    public void addPhysicalAction(String fieldName, Object value, boolean canBeReferenced) {
      parent.addPhysicalAction(fieldName, value, canBeReferenced);
    }

    public void addPhysicalAction(String fieldName, Object value) {
      parent.addPhysicalAction(fieldName, value);
    }

    public void addSubArrayAction(int start, Object array, int length) {
      parent.addSubArrayAction(start, array, length);
    }

    public void addMetaData(MetaDataDescriptorInternal md) {
      if (metaDataOffset == UNINITIALIZED) {
        metaDataOffset = output.getBytesWritten() - startMark.getPosition();
      }
      parent.addMetaData(md, true);
    }

    public int getActionCount() {
      throw new UnsupportedOperationException();
    }

    public void setArrayLength(int length) {
      throw new UnsupportedOperationException();
    }

    public void setParentObjectID(ObjectID id) {
      throw new UnsupportedOperationException();
    }

    public DNAWriter createAppender() {
      throw new UnsupportedOperationException();
    }

    public boolean isContiguous() {
      return parent.isContiguous();
    }

    public void markSectionEnd() {
      appendSectionLength = output.getBytesWritten() - startMark.getPosition();
      parent.appenderSectionEnd(appendSectionLength);
    }

    public void copyTo(TCByteBufferOutput dest) {
      startMark.copyTo(dest, appendSectionLength);
    }

    public void finalizeHeader() {
      throw new UnsupportedOperationException();
    }
  }

}
