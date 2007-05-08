/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.OrderedEventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectDNAImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class ObjectSyncMessage extends AbstractGroupMessage implements OrderedEventContext {

  public static final int        MANAGED_OBJECT_SYNC_TYPE = 0;

  private Set                    oids;
  private int                    dnaCount;
  private TCByteBuffer[]         dnas;
  private ObjectStringSerializer serializer;
  private Map                    rootsMap;
  private long                   sequenceID;

  public ObjectSyncMessage() {
    // Make serialization happy
    super(-1);
  }

  public ObjectSyncMessage(int type) {
    super(type);
  }

  protected void basicReadExternal(int msgType, ObjectInput in) throws IOException {
    Assert.assertEquals(MANAGED_OBJECT_SYNC_TYPE, msgType);
    readObjectIDS(in);
    dnaCount = in.readInt();
    readRootsMap(in);
    serializer = readObjectStringSerializer(in);
    this.dnas = readByteBuffers(in);
    this.sequenceID = in.readLong();
  }

  protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
    Assert.assertEquals(MANAGED_OBJECT_SYNC_TYPE, msgType);
    writeObjectIDS(out);
    out.writeInt(dnaCount);
    writeRootsMap(out);
    writeObjectStringSerializer(out, serializer);
    writeByteBuffers(out, dnas);
    recycle(dnas);
    dnas = null;
    out.writeLong(this.sequenceID);
  }

  private void writeRootsMap(ObjectOutput out) throws IOException {
    out.writeInt(rootsMap.size());
    for (Iterator i = rootsMap.entrySet().iterator(); i.hasNext();) {
      Entry e = (Entry) i.next();
      out.writeUTF((String) e.getKey());
      out.writeLong(((ObjectID) e.getValue()).toLong());
    }
  }

  private void readRootsMap(ObjectInput in) throws IOException {
    int size = in.readInt();
    if (size == 0) {
      this.rootsMap = Collections.EMPTY_MAP;
    } else {
      this.rootsMap = new HashMap(size);
      for (int i = 0; i < size; i++) {
        this.rootsMap.put(in.readUTF(), new ObjectID(in.readLong()));
      }
    }
  }

  private void recycle(TCByteBuffer[] buffers) {
    for (int i = 0; i < buffers.length; i++) {
      buffers[i].recycle();
    }
  }

  private void writeObjectIDS(ObjectOutput out) throws IOException {
    out.writeInt(oids.size());
    for (Iterator i = oids.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      out.writeLong(oid.toLong());
    }
  }

  private void readObjectIDS(ObjectInput in) throws IOException {
    int size = in.readInt();
    oids = new HashSet(size);
    for (int i = 0; i < size; i++) {
      oids.add(new ObjectID(in.readLong()));
    }
  }

  public void initialize(Set dnaOids, int count, TCByteBuffer[] serializedDNAs,
                         ObjectStringSerializer objectSerializer, Map roots, long sID) {
    this.oids = dnaOids;
    this.dnaCount = count;
    this.dnas = serializedDNAs;
    this.serializer = objectSerializer;
    this.rootsMap = roots;
    this.sequenceID = sID;
  }

  public int getDnaCount() {
    return dnaCount;
  }

  public Set getOids() {
    return oids;
  }

  public ObjectStringSerializer getSerializer() {
    return serializer;
  }

  public Map getRootsMap() {
    return rootsMap;
  }

  /**
   * This method calls returns a list of DNAs that can be applied to ManagedObjects. This method could only be called
   * once. It throws an AssertionError if you ever call this twice
   */
  public List getDNAs() {
    Assert.assertNotNull(this.dnas);
    TCByteBufferInputStream toi = new TCByteBufferInputStream(this.dnas);
    ArrayList objectDNAs = new ArrayList(dnaCount);
    for (int i = 0; i < dnaCount; i++) {
      ObjectDNAImpl dna = new ObjectDNAImpl(serializer, false);
      try {
        dna.deserializeFrom(toi);
      } catch (IOException e) {
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
    TCByteBuffer[] tcbb = new TCByteBuffer[dnas.length];
    for (int i = 0; i < dnas.length; i++) {
      tcbb[i] = dnas[i];
    }
    return tcbb;
  }

  public long getSequenceID() {
    return this.sequenceID;
  }
}
