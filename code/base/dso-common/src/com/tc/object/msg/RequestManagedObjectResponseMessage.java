/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.dna.impl.ObjectDNAImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.session.SessionID;
import com.tc.util.CommonShutDownHook;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author steve
 */
public class RequestManagedObjectResponseMessage extends DSOMessageBase implements EventContext {

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

  public RequestManagedObjectResponseMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutput out, MessageChannel channel,
                                             TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public RequestManagedObjectResponseMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header,
                                             TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public Collection getObjects() {
    return Collections.unmodifiableCollection(objects);
  }

  public void initialize(TCByteBuffer[] dnas, int count, ObjectStringSerializer aSerializer, long bid, int tot) {
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

  protected void dehydrateValues() {
    putNVPair(SERIALIZER_ID, serializer);
    putNVPair(BATCH_ID, batchID);
    putNVPair(TOTAL_ID, total);
    putNVPair(DNA_COUNT, dnaCount);
    putNVPair(DNA_DATA, dnaData);
    dnaData = null;
  }

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
        this.serializer = (ObjectStringSerializer) getObject(new ObjectStringSerializer());
        return true;
      default:
        return false;
    }
  }

  static int rcount;
  static int bufferCount;
  static {
    CommonShutDownHook.addShutdownHook(new Runnable() {
      public void run() {
        logger.info("No of times Buffers wasted = " + rcount + " Buffers wasted count = " + bufferCount);
      }
    });
  }

  // TODO :: It is recycled only on write. Not on read.
  public void doRecycleOnRead() {
    rcount++;
    bufferCount += getEntireMessageData().length;
  }
}
