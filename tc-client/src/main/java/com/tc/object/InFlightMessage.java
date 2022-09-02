/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object;

import com.tc.net.protocol.TCNetworkMessage;
import com.tc.tracing.Trace;
import com.tc.entity.VoltronEntityMessage;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;
import static com.tc.object.StatType.CLIENT_COMPLETE;
import static com.tc.object.StatType.CLIENT_DECODED;
import static com.tc.object.StatType.CLIENT_ENCODE;
import static com.tc.object.StatType.CLIENT_GOT;
import static com.tc.object.StatType.CLIENT_RECEIVED;
import static com.tc.object.StatType.CLIENT_RETIRED;
import static com.tc.object.StatType.CLIENT_SEND;
import static com.tc.object.StatType.CLIENT_SENT;
import static com.tc.object.StatType.SERVER_ADD;
import static com.tc.object.StatType.SERVER_BEGININVOKE;
import static com.tc.object.StatType.SERVER_COMPLETE;
import static com.tc.object.StatType.SERVER_ENDINVOKE;
import static com.tc.object.StatType.SERVER_RECEIVED;
import static com.tc.object.StatType.SERVER_RETIRED;
import static com.tc.object.StatType.SERVER_SCHEDULE;
import static java.util.Objects.requireNonNull;

import com.tc.text.PrettyPrintable;
import java.util.ArrayList;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import com.tc.net.protocol.tcm.TCAction;
import org.terracotta.entity.InvocationCallback;


/**
 * This is essentially a wrapper over an in-flight VoltronEntityMessage, used for tracking its response.
 * The message is stored here, since it is sent asynchronously, along with storage for the return value.
 * Note that this is only used from within ClietEntityManagerImpl, and was originally embedded there, but was extracted to
 * make unit testing more direct.
 */
public class InFlightMessage implements PrettyPrintable {
  private final VoltronEntityMessage message;
  private final EntityID eid;
  private final InvocationCallback<byte[]> callback;

  private final EnumSet<VoltronEntityMessage.Acks> outstandingAcks = EnumSet.allOf(VoltronEntityMessage.Acks.class);

  private boolean isSent;
  private Exception exception;
  private byte[] value;
  private final Trace trace;

  private final AtomicReference<State> state = new AtomicReference<>(State.PENDING);

  private volatile long start;
  private volatile long send;
  private volatile long notifySent;
  private volatile long sent;
  private volatile long received;
  private volatile long complete;
  private volatile long retired;
  private volatile long end;

  private long[] serverStats;

  private TCNetworkMessage networkMessage;

  public InFlightMessage(EntityID eid, Supplier<? extends VoltronEntityMessage> message, InvocationCallback<byte[]> callback) {
    this.eid = requireNonNull(eid);
    this.message = requireNonNull(message.get());
    this.callback = callback;
    this.trace = Trace.newTrace(this.message, "InFlightMessage");
  }
  
  void setStatisticsBoundries(long start, long end) {
    this.start = start;
    this.end = end;
  }
  
  public long[] collect() {
    long[] stats = new long[StatType.END.ordinal()];
    if (stats != null) {
      stats[StatType.CLIENT_ENCODE.ordinal()] = start;
      stats[StatType.CLIENT_SEND.ordinal()] = send;
      stats[StatType.CLIENT_SENT.ordinal()] = sent;
      stats[StatType.CLIENT_COMPLETE.ordinal()] = complete;
      stats[StatType.CLIENT_RECEIVED.ordinal()] = received;
      stats[StatType.CLIENT_RETIRED.ordinal()] = retired;
      stats[StatType.CLIENT_DECODED.ordinal()] = end;
      if (serverStats != null) {
        stats[StatType.SERVER_ADD.ordinal()] = serverStats[StatType.SERVER_ADD.serverSpot()];
        stats[StatType.SERVER_SCHEDULE.ordinal()] = serverStats[StatType.SERVER_SCHEDULE.serverSpot()];
        stats[StatType.SERVER_BEGININVOKE.ordinal()] = serverStats[StatType.SERVER_BEGININVOKE.serverSpot()];
        stats[StatType.SERVER_ENDINVOKE.ordinal()] = serverStats[StatType.SERVER_ENDINVOKE.serverSpot()];
        stats[StatType.SERVER_RECEIVED.ordinal()] = serverStats[StatType.SERVER_RECEIVED.serverSpot()];
        stats[StatType.SERVER_RETIRED.ordinal()] = serverStats[StatType.SERVER_RETIRED.serverSpot()];
        stats[StatType.SERVER_COMPLETE.ordinal()] = serverStats[StatType.SERVER_COMPLETE.serverSpot()];
      }
    }
    return stats;
  }
  
  @Override
  public Map<String, ?> getStateMap() {
    List<InFlightStats.Combo> values = new ArrayList<>(20);
    long[] collect = collect();
    values.add(new InFlightStats.Combo(CLIENT_ENCODE, CLIENT_SEND).add(collect));
    values.add(new InFlightStats.Combo(CLIENT_SEND, CLIENT_SENT).add(collect));
    values.add(new InFlightStats.Combo(CLIENT_SENT, CLIENT_RECEIVED).add(collect));
    values.add(new InFlightStats.Combo(CLIENT_RECEIVED, CLIENT_COMPLETE).add(collect));
    values.add(new InFlightStats.Combo(CLIENT_COMPLETE, CLIENT_GOT).add(collect));
    values.add(new InFlightStats.Combo(CLIENT_GOT, CLIENT_DECODED).add(collect));
    values.add(new InFlightStats.Combo(CLIENT_COMPLETE, CLIENT_RETIRED).add(collect));
    values.add(new InFlightStats.Combo(CLIENT_SENT, CLIENT_RETIRED).add(collect));
    values.add(new InFlightStats.Combo(CLIENT_ENCODE, CLIENT_DECODED).add(collect));
    values.add(new InFlightStats.Combo(SERVER_ADD, SERVER_SCHEDULE).add(collect));
    values.add(new InFlightStats.Combo(SERVER_SCHEDULE, SERVER_BEGININVOKE).add(collect));
    values.add(new InFlightStats.Combo(SERVER_BEGININVOKE, SERVER_ENDINVOKE).add(collect));
    values.add(new InFlightStats.Combo(SERVER_RECEIVED, SERVER_COMPLETE).add(collect));
    values.add(new InFlightStats.Combo(SERVER_COMPLETE, SERVER_RETIRED).add(collect));

    Map<String, Object> map = new LinkedHashMap<>();
    map.put("entity", eid);
    Map<String, Object> timing = new LinkedHashMap<>();
    timing.put("send", send);
    timing.put("sent", sent);
    timing.put("notifySent", notifySent);
    timing.put("received", received);
    timing.put("complete", complete);
    timing.put("retired", retired);
    map.put("marks", timing);
    Map<String, Object> offset = new LinkedHashMap<>();
    values.forEach(c->offset.put(c.toString(), c.value()));
    map.put("timing", offset);
    return map;
  }

  public EntityID getEntityID() {
    return eid;
  }

  /**
   * Used when populating the reconnect handshake.
   */
  public VoltronEntityMessage getMessage() {
    return this.message;
  }

  public TransactionID getTransactionID() {
    return this.message.getTransactionID();
  }

  public boolean send() {
    Trace.activeTrace().log("InFlightMessage.send()");
    Assert.assertFalse(this.isSent);
    this.isSent = true;
    this.send = System.nanoTime();
    try {
      return (this.networkMessage = ((TCAction) this.message).send()) != null;
    } finally {
      this.sent = System.nanoTime();
    }
  }
  
  public synchronized void sent() {
    if (ackDelivered(VoltronEntityMessage.Acks.SENT)) {
      this.notifySent = System.nanoTime();
    }
  }

  public synchronized void received() {
    if (ackDelivered(VoltronEntityMessage.Acks.RECEIVED)) {
      this.received = System.nanoTime();
    }
  }

  public synchronized boolean isSent() {
    return this.isSent;
  }
  
  public void setResult(byte[] value, Exception error) {
    if (Trace.isTraceEnabled()) {
      trace.log("Received Result: " + value + " ; Exception: " + (error != null ? error.getLocalizedMessage() : "None"));
    }
    if (ackDelivered(VoltronEntityMessage.Acks.RECEIVED)) {
      this.received = System.nanoTime();
    }
    if (outstandingAcks.contains(VoltronEntityMessage.Acks.COMPLETED)) {
      try {
        if (error != null) {
          Assert.assertNull(value);
          this.exception = error;
          handleException(this.exception);
        } else {
          Assert.assertNotNull(value);
          this.value = value;
          handleMessage(value);
        }
      } finally {
        this.complete = System.nanoTime();
        ackDelivered(VoltronEntityMessage.Acks.COMPLETED);
      }
    }
  }
  
  void handleException(Exception ee) {
    callback.failure(ExceptionUtils.convert(ee));
  }

  void handleMessage(byte[] raw) {
    callback.result(raw);
  }

  private boolean ackDelivered(VoltronEntityMessage.Acks ack) {
    if (Trace.isTraceEnabled()) {
      trace.log("Received ACK: " + ack);
    }
    if (this.outstandingAcks.remove(ack)) {
      switch (ack) {
        case SENT:
          callback.sent();
          break;
        case RECEIVED:
          callback.received();
          break;
        case COMPLETED:
          callback.complete();
          break;
        case RETIRED:
          callback.retired();
          break;
      }
      return true;
    } else {
      return false;
    }
  }

  public void retired() {
    if (ackDelivered(VoltronEntityMessage.Acks.RETIRED)) {
      this.retired = System.nanoTime();
      if (message.getVoltronType() == VoltronEntityMessage.Type.INVOKE_ACTION) {
        Assert.assertTrue("failed " + this.message.getTransactionID(), value != null || exception != null);
      }
    }
  }

  void addServerStatistics(long[] stats) {
    this.serverStats = stats;
  }

  public boolean commit() {
    return state.compareAndSet(State.PENDING, State.COMMITTED) || State.COMMITTED.equals(state.get());
  }

  public boolean cancel() {
    return (networkMessage == null || networkMessage.cancel()) && (state.compareAndSet(State.PENDING, State.CANCELLED) || State.CANCELLED.equals(state.get()));
  }

  enum State {
    PENDING,
    COMMITTED,
    CANCELLED;
  }
}
