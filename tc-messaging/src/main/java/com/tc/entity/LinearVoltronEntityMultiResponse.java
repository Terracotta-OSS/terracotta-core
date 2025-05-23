/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.entity;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NetworkRecall;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientInstanceID;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;



public class LinearVoltronEntityMultiResponse extends DSOMessageBase implements VoltronEntityMultiResponse {
  
  private final byte OP_ID = 1;
  private final byte DONE_ID = 2;
  
  public enum Operation {
    RECEIVED,
    RETIRED,
    RESULT {
      boolean hasData() {
        return true;
      }
    },
    SERVER_MESSAGE{
      boolean hasData() {
        return true;
      }
    },
    INVOKE_MESSAGE {
      boolean hasData() {
        return true;
      }
    },
    RESULT_RETIRED {
      boolean hasData() {
        return true;
      }
    },
    DONE,
    STATS {
      boolean hasData() {
        return true;
      }
    };
    
    boolean hasData() {
      return false;
    }
  }  
    
  private List<Op> timeline = new LinkedList<>();

  private boolean stopAdding;
  
  private static class Op {
    private final Operation type;
    private final long id;
    private final byte[] data;

    public Op(Operation type, long id, byte[] data) {
      this.type = type;
      this.id = id;
      this.data = data;
    }
    
    public Op(byte[] raw) {
      ByteBuffer reader = ByteBuffer.wrap(raw);
      this.type = Operation.values()[reader.getInt()];
      this.id = reader.getLong();
      this.data = new byte[reader.remaining()];
      reader.get(this.data);
    }
    
    public byte[] convert() {
      ByteBuffer buffer = ByteBuffer.allocate(4 + 8 + data.length);
      buffer.putInt(type.ordinal());
      buffer.putLong(id);
      if (data != null) {
        buffer.put(data);
      }
      return buffer.array();
    }
  }

  @Override
  public int replay(ReplayReceiver receiver) {
    int count = 0;
    for (Op op : timeline) {
      switch(op.type) {
        case INVOKE_MESSAGE:
          receiver.message(new TransactionID(op.id), op.data);
          break;
        case RECEIVED:
          receiver.received(new TransactionID(op.id));
          break;
        case RESULT:
          receiver.result(new TransactionID(op.id), op.data);
          break;
        case SERVER_MESSAGE:
          receiver.message(new ClientInstanceID(op.id), op.data);
          break;
        case RESULT_RETIRED:
          receiver.result(new TransactionID(op.id), op.data);
      //  fallthrough
        case RETIRED:
          receiver.retired(new TransactionID(op.id));
          break;
        case DONE:
          receiver.stats(TransactionID.NULL_ID, new long[] {op.id});
          break;
        case STATS:
          ByteBuffer data = ByteBuffer.wrap(op.data);
          long[] vals = new long[data.remaining() / Long.BYTES];
          for (int x=0;x<vals.length;x++) {
            vals[x] = data.getLong();
          }
          receiver.stats(new TransactionID(op.id), vals);
          break;
        default:
          throw new AssertionError("unknown op");
      }
      count+=1;
    }
    return count;
  }
  
  
  
  
  public LinearVoltronEntityMultiResponse(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public LinearVoltronEntityMultiResponse(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBufferInputStream data) {
    super(sessionID, monitor, channel, header, data);
  }
  
  @Override
  public synchronized NetworkRecall send() {
    return super.send();
  }

  @Override
  public boolean addReceived(TransactionID tid) {
    return buildOp(Operation.RECEIVED, tid.toLong(), null);
  }
  
  private synchronized boolean buildOp(Operation type, long id, byte[] data) {
    if (!stopAdding) {
      Op op = new Op(type, id, data);
      timeline.add(op);
      switch (type) {
        case DONE:
          stopAdding = true;
          // fallthrough
        case RESULT_RETIRED:
        case RECEIVED:
        case RESULT:
          immediateSend = true;
          break;
        default:
      }
      return true;
    }

    return false;
  }
  
  @Override
  public boolean addRetired(TransactionID tid) {
    return buildOp(Operation.RETIRED, tid.toLong(), null);
  }

  @Override
  public boolean addResult(TransactionID tid, byte[] result) {
    return buildOp(Operation.RESULT, tid.toLong(), result);
  }

  @Override
  public boolean addResultAndRetire(TransactionID tid, byte[] result) {
    return buildOp(Operation.RESULT_RETIRED, tid.toLong(), result);
  }

  @Override
  public boolean addServerMessage(ClientInstanceID cid, byte[] message) {
    return buildOp(Operation.SERVER_MESSAGE, cid.getID(), message);
  }

  @Override
  public boolean addServerMessage(TransactionID cid, byte[] message) {
    return buildOp(Operation.INVOKE_MESSAGE, cid.toLong(), message);
  }

  @Override
  public boolean addStats(TransactionID cid, long[] timings) {
    ByteBuffer buffer = ByteBuffer.allocate(timings.length * Long.BYTES);
    for (long t : timings) {
      buffer.putLong(t);
    }
    return buildOp(Operation.STATS, cid.toLong(), buffer.array());
  }
  
  @Override
  public void stopAdding() {
    stopAddingTime = System.nanoTime();
    buildOp(Operation.DONE, stopAddingTime - startAddingTime, null);
    timeline = Collections.unmodifiableList(timeline);
  }
  
  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    if (name == OP_ID) {
      Operation type = Operation.values()[getShortValue()];
      long id = getLongValue();
      byte[] data = null;
      if (type.hasData()) {
        int len = getIntValue();
        data = new byte[len];
        getInputStream().readFully(data);
      }
      timeline.add(new Op(type, id, data));
      return true;
    } else if (name == DONE_ID) {
      Assert.assertEquals(getIntValue(), timeline.size());
      return true;
    }
    return false;
  }

  @Override
  protected void dehydrateValues() {
    int count = 0;
    for (Op op : timeline) {
      putNVPair(OP_ID, (short)op.type.ordinal());
      getOutputStream().writeLong(op.id);
      if (op.type.hasData()) {
        getOutputStream().writeInt(op.data.length);
        getOutputStream().write(op.data);
      }
      count++;
    }
    putNVPair(DONE_ID,count);
    
  }
  
  private long stopAddingTime;
  private long startAddingTime;
  private boolean started = false;

  @Override
  public synchronized boolean startAdding() {
    if (!started) {
      startAddingTime = System.nanoTime();
      started = true;
      return true;
    }
    return false;
  }
  // the setter and getter on this variable is always the same 
  // thread and it is used for a minor optimation which does not affect correctness.
  // As a result it does not need synchronization or volatile  
  private boolean immediateSend = false;
  
  @Override
  public boolean shouldSend() {
    return immediateSend;
  }
}
