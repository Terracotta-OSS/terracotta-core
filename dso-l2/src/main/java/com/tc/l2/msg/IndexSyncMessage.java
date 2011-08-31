/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.OrderedEventContext;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.util.Assert;

import java.io.IOException;

public class IndexSyncMessage extends AbstractGroupMessage implements OrderedEventContext {

  public static final int INDEX_SYNC_TYPE = 0;

  private String          cacheName;
  private String          fileName;
  private int             length;
  private byte[]          data;
  private long            sequenceID;
  private boolean         isTCFile;
  private boolean         isLast;

  public IndexSyncMessage() {
    // Make serialization happy
    super(-1);
  }

  public IndexSyncMessage(final int type) {
    super(type);
  }

  public void initialize(final String cName, final String fName, final byte[] fileData, long sID, boolean tcFile,
                         boolean last) {
    this.cacheName = cName;
    this.fileName = fName;
    this.length = fileData.length;
    this.data = fileData;
    this.sequenceID = sID;
    this.isTCFile = tcFile;
    this.isLast = last;
  }

  @Override
  protected void basicDeserializeFrom(final TCByteBufferInput in) throws IOException {
    Assert.assertEquals(INDEX_SYNC_TYPE, getType());
    this.cacheName = in.readString();
    this.length = in.readInt();
    this.fileName = in.readString();
    this.sequenceID = in.readLong();
    this.isLast = in.readBoolean();
    this.isTCFile = in.readBoolean();
    this.data = new byte[this.length];
    in.read(this.data);
  }

  @Override
  protected void basicSerializeTo(final TCByteBufferOutput out) {
    Assert.assertEquals(INDEX_SYNC_TYPE, getType());
    out.writeString(this.cacheName);
    out.writeInt(this.length);
    out.writeString(this.fileName);
    out.writeLong(this.sequenceID);
    out.writeBoolean(this.isLast);
    out.writeBoolean(this.isTCFile);
    out.write(this.data);
  }

  public String getCacheName() {
    return this.cacheName;
  }

  public String getFileName() {
    return this.fileName;
  }

  public byte[] getData() {
    return this.data;
  }

  public boolean isTCFile() {
    return this.isTCFile;
  }

  public boolean isLast() {
    return this.isLast;
  }

  public long getSequenceID() {
    return this.sequenceID;
  }

}
