/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.OrderedEventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.impl.ObjectDNAImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;
import com.tc.util.BasicObjectIDSet;
import com.tc.util.ObjectIDSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ObjectSyncMessage extends AbstractGroupMessage implements OrderedEventContext {

  public static final int        MANAGED_OBJECT_SYNC_TYPE = 0;

  private ObjectIDSet            deletedOids;
  private ObjectIDSet            oids;
  private int                    dnaCount;
  private TCByteBuffer[]         dnas;
  private ObjectStringSerializer serializer;
  private Map<String, ObjectID>  rootsMap;
  private long                   sequenceID;
  private ServerTransactionID    servertxnID;

  public ObjectSyncMessage() {
    // Make serialization happy
    super(-1);
  }

  public ObjectSyncMessage(ServerTransactionID stxnID, ObjectIDSet dnaOids, int count,
                         TCByteBuffer[] serializedDNAs, ObjectStringSerializer objectSerializer,
                         Map<String, ObjectID> roots, long sqID, ObjectIDSet deletedObjectIds) {
    super(MANAGED_OBJECT_SYNC_TYPE);
    this.servertxnID = stxnID;
    this.oids = dnaOids;
    this.dnaCount = count;
    this.dnas = serializedDNAs;
    this.serializer = objectSerializer;
    this.rootsMap = roots;
    this.sequenceID = sqID;
    this.deletedOids = deletedObjectIds;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    Assert.assertEquals(MANAGED_OBJECT_SYNC_TYPE, getType());
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer = nodeIDSerializer.deserializeFrom(in);
    this.servertxnID = new ServerTransactionID(nodeIDSerializer.getNodeID(), new TransactionID(in.readLong()));
    this.oids = new BasicObjectIDSet();
    this.oids.deserializeFrom(in);
    this.dnaCount = in.readInt();
    readRootsMap(in);
    this.serializer = new ObjectStringSerializerImpl();
    this.serializer.deserializeFrom(in);
    this.dnas = readByteBuffers(in);
    this.sequenceID = in.readLong();
    deletedOids = new BasicObjectIDSet();
    deletedOids.deserializeFrom(in);
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    Assert.assertEquals(MANAGED_OBJECT_SYNC_TYPE, getType());
    final NodeIDSerializer nodeIDSerializer = new NodeIDSerializer(this.servertxnID.getSourceID());
    nodeIDSerializer.serializeTo(out);
    out.writeLong(this.servertxnID.getClientTransactionID().toLong());
    this.oids.serializeTo(out);
    out.writeInt(this.dnaCount);
    writeRootsMap(out);
    this.serializer.serializeTo(out);
    writeByteBuffers(out, this.dnas);
    recycle(this.dnas);
    this.dnas = null;
    out.writeLong(this.sequenceID);
    deletedOids.serializeTo(out);
  }

  private void writeRootsMap(TCByteBufferOutput out) {
    out.writeInt(this.rootsMap.size());
    for (Entry<String, ObjectID> entry : this.rootsMap.entrySet()) {      
      out.writeString(entry.getKey());
      out.writeLong(entry.getValue().toLong());
    }
  }

  private void readRootsMap(TCByteBufferInput in) throws IOException {
    final int size = in.readInt();
    if (size == 0) {
      this.rootsMap = Collections.emptyMap();
    } else {
      this.rootsMap = new HashMap<String, ObjectID>(size);
      for (int i = 0; i < size; i++) {
        this.rootsMap.put(in.readString(), new ObjectID(in.readLong()));
      }
    }
  }

  private void recycle(TCByteBuffer[] buffers) {
    for (final TCByteBuffer buffer : buffers) {
      buffer.recycle();
    }
  }

  public int getDnaCount() {
    return this.dnaCount;
  }

  public ObjectIDSet getOids() {
    return this.oids;
  }

  public Map<String, ObjectID> getRootsMap() {
    return this.rootsMap;
  }

  /**
   * This method calls returns a list of DNAs that can be applied to ManagedObjects. This method could only be called
   * once. It throws an AssertionError if you ever call this twice
   */
  @SuppressWarnings("resource")
  public List<DNA> getDNAs() {
    Assert.assertNotNull(this.dnas);
    final TCByteBufferInputStream toi = new TCByteBufferInputStream(this.dnas);
    final List<DNA> objectDNAs = new ArrayList<DNA>(this.dnaCount);
    for (int i = 0; i < this.dnaCount; i++) {
      final ObjectDNAImpl dna = new ObjectDNAImpl(this.serializer, false);
      try {
        dna.deserializeFrom(toi);
      } catch (final IOException e) {
        throw new AssertionError(e);
      }
      Assert.assertFalse(dna.isDelta());
      objectDNAs.add(dna);
    }
    this.dnas = null;
    return objectDNAs;
  }

  /*
   * For testing only
   */
  public TCByteBuffer[] getUnprocessedDNAs() {
    final TCByteBuffer[] tcbb = new TCByteBuffer[this.dnas.length];
    for (int i = 0; i < this.dnas.length; i++) {
      tcbb[i] = this.dnas[i];
    }
    return tcbb;
  }

  @Override
  public long getSequenceID() {
    return this.sequenceID;
  }

  public ServerTransactionID getServerTransactionID() {
    return this.servertxnID;
  }

  public ObjectIDSet getDeletedOids() {
    return deletedOids;
  }
}
