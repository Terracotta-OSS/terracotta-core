/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.dna.impl.ObjectDNAImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author steve
 */
public class RequestManagedObjectResponseMessageImpl extends DSOMessageBase implements
    RequestManagedObjectResponseMessage {

  private final static byte      SERIALIZER_ID = 1;
  private final static byte      TOTAL_ID      = 2;
  private final static byte      BATCH_ID      = 3;
  private final static byte      DNA_COUNT     = 4;
  private final static byte      DNA_DATA      = 5;

  private Collection             objects;
  private ObjectStringSerializer serializer;
  private int                    total;
  private long                   batchID;
  private TCByteBuffer[]         dnaData;
  private int                    dnaCount;

  public RequestManagedObjectResponseMessageImpl(SessionID sessionID, MessageMonitor monitor,
                                                 TCByteBufferOutputStream out, MessageChannel channel,
                                                 TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public RequestManagedObjectResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                                 TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public Collection getObjects() {
    return Collections.unmodifiableCollection(objects);
  }

  public void initialize(TCByteBuffer[] dnas, int count, ObjectStringSerializer aSerializer, long bid, int tot) {
    // System.err.println("SARO : dna count = " + count + " dnas[] = " + dnas.length + " tot = " + tot);
    // for (int i = 0; i < dnas.length; i++) {
    // System.err.println("SARO : "+ i + " : " + dnas[i].capacity());
    // }
    this.dnaCount = count;
    this.dnaData = dnas;
    this.serializer = aSerializer;
    this.batchID = bid;
    this.total = tot;
  }

  public ObjectStringSerializer getSerializer() {
    return serializer;
  }

  public long getBatchID() {
    return batchID;
  }

  public int getTotal() {
    return total;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(SERIALIZER_ID, serializer);
    putNVPair(BATCH_ID, batchID);
    putNVPair(TOTAL_ID, total);
    putNVPair(DNA_COUNT, dnaCount);
    putNVPair(DNA_DATA, dnaData);
    dnaData = null;
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case DNA_DATA: {
        for (int i = 0, n = dnaCount; i < n; i++) {
          objects.add(getObject(new ObjectDNAImpl(serializer, false)));
        }
        return true;
      }
      case DNA_COUNT:
        dnaCount = getIntValue();
        this.objects = new ArrayList(dnaCount);
        return true;
      case BATCH_ID:
        this.batchID = getLongValue();
        return true;
      case TOTAL_ID:
        this.total = getIntValue();
        return true;
      case SERIALIZER_ID:
        this.serializer = (ObjectStringSerializer) getObject(new ObjectStringSerializerImpl());
        return true;
      default:
        return false;
    }
  }

  @Override
  public void doRecycleOnRead() {
    // TODO :: It is recycled only on write. Not on read.
  }
}
