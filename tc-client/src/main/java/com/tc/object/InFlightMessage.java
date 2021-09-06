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

import org.terracotta.exception.EntityException;

import com.tc.tracing.Trace;
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.protocol.tcm.TCMessage;
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
import com.tc.text.PrettyPrintable;
import java.util.ArrayList;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;


/**
 * This is essentially a wrapper over an in-flight VoltronEntityMessage, used for tracking its response.
 * The message is stored here, since it is sent asynchronously, along with storage for the return value.
 * Note that this is only used from within ClietEntityManagerImpl, and was originally embedded there, but was extracted to
 * make unit testing more direct.
 */
public class InFlightMessage implements PrettyPrintable {
  private final VoltronEntityMessage message;
  private final EntityID eid;
  private final InFlightMonitor monitor;
  private final boolean asyncMode;
  /**
   * The set of pending ACKs determines when the caller returns from the send, in order to preserve ordering in the
   * client code.  This is different from being "done" which specifically means that the COMPLETED has happened,
   * potentially returning a value or exception.
   * ACKs are removed from this pending set, as they arrive.
   */
  private final Set<VoltronEntityMessage.Acks> pendingAcks;
  // set of all acks for bookkeeping
  private final EnumSet<VoltronEntityMessage.Acks> outstandingAcks = EnumSet.allOf(VoltronEntityMessage.Acks.class);
  // Note that the point where we wait for acks isn't exposed outside the InvokeFuture interface so this set of waiting
  // threads only applies to those threads waiting to get a response.
  private final Set<Thread> waitingThreads;

  private boolean isSent;
  private Exception exception;
  private byte[] value;
  private boolean getCanComplete;
  private final boolean blockGetOnRetired;
  private final Trace trace;
  
  private Runnable runOnRetire;
    
  private long start;
  private long send;
  private long notifySent;
  private long sent;
  private long received;
  private long got;
  private long complete;
  private long retired;
  private long end;
  
  private long[] serverStats;
  
  public InFlightMessage(EntityID eid, Supplier<? extends VoltronEntityMessage> message, Set<VoltronEntityMessage.Acks> acks, InFlightMonitor monitor, boolean shouldBlockGetOnRetire, boolean asyncMode) {
    this.eid = eid;
    this.message = message.get();
    this.monitor = monitor;
    this.asyncMode = asyncMode;
    Assert.assertNotNull(eid);
    Assert.assertNotNull(message);
    this.pendingAcks = EnumSet.noneOf(VoltronEntityMessage.Acks.class);
    this.pendingAcks.addAll(acks);
    this.waitingThreads = new HashSet<>();
    this.blockGetOnRetired = shouldBlockGetOnRetire;
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
      stats[StatType.CLIENT_GOT.ordinal()] = got;
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
  
  synchronized void runOnRetire(Runnable r) {
    if (retired != 0) {
      r.run();
    } else {
      this.runOnRetire = r;
    }
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
    timing.put("got", got);
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
      return ((TCMessage)this.message).send();
    } finally {
      this.sent = System.nanoTime();
    }
  }
  
  public void waitForAcks() {
    boolean interrupted = false;
    boolean complete = false;
    try {
      while (!complete) {
        try {
          waitForAcks(0, TimeUnit.MILLISECONDS);
          complete = true;
        } catch (InterruptedException ie) {
          interrupted = true;
        } catch (TimeoutException te) {
          throw new AssertionError(te);
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }
  
  public void waitForAcks(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
    Trace.activeTrace().log("InFlightMessage.waitForAcks");
    timedWait(() -> pendingAcks.isEmpty(), timeout, unit);
  }
  
  public synchronized void sent() {
    if (ackDelivered(VoltronEntityMessage.Acks.SENT)) {
      this.notifySent = System.nanoTime();
      if (this.pendingAcks.isEmpty()) {
        notifyAll();
      }
    }
  }

  public synchronized void received() {
    if (ackDelivered(VoltronEntityMessage.Acks.RECEIVED)) {
      this.received = System.nanoTime();
      if (this.pendingAcks.isEmpty()) {
        notifyAll();
      }
    }
  }

  public synchronized void interrupt() {
    for (Thread waitingThread : this.waitingThreads) {
      waitingThread.interrupt();
    }
  }

  public synchronized boolean isDone() {
    return this.getCanComplete;
  }

  public synchronized boolean isSent() {
    return this.isSent;
  }
  
  public synchronized byte[] get() throws InterruptedException, EntityException {
    try {
      return getWithTimeout(0, TimeUnit.MILLISECONDS);
    } catch (TimeoutException to) {
    // should not happpen with zero timeout
      throw new AssertionError(to);
    }
  }
  
  private synchronized void timedWait(Callable<Boolean> predicate, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
   Thread callingThread = Thread.currentThread();
    boolean didAdd = this.waitingThreads.add(callingThread);
    // We can't have already been waiting.
    Assert.assertTrue(didAdd);
    
    long end = (timeout > 0) ? System.nanoTime() + unit.toNanos(timeout) : 0;
    try {
      while (!predicate.call()) {
        long timing = (end > 0) ? end - System.nanoTime() : 0;
        if (timing < 0) {
          throw new TimeoutException();
        } else {
          wait(timing / TimeUnit.MILLISECONDS.toNanos(1), (int)(timing % TimeUnit.MILLISECONDS.toNanos(1)));
        }
      }
    } catch (InterruptedException | TimeoutException ie) {
      throw ie;
    } catch (Exception exp) {
      throw new AssertionError(exp);
    } finally {
      this.waitingThreads.remove(callingThread);
    }
  }

  public byte[] getWithTimeout(long timeout, TimeUnit unit) throws InterruptedException, EntityException, TimeoutException {
    trace.log("getWithTimeout()");
    timedWait(this::isDone, timeout, unit);
    this.got = System.nanoTime();
    if (exception != null) {
      ExceptionUtils.throwEntityException(exception);
      throw new AssertionError("exception conversion failed", exception);
    } else {
      if (this.message.getVoltronType() == VoltronEntityMessage.Type.INVOKE_ACTION) {
        Assert.assertNotNull(value);
      }
      return value;
    }
  }

  public synchronized void setResult(byte[] value, Exception error) {
    if (Trace.isTraceEnabled()) {
      trace.log("Received Result: " + value + " ; Exception: " + (error != null ? error.getLocalizedMessage() : "None"));
    }
    if (ackDelivered(VoltronEntityMessage.Acks.RECEIVED)) {
      this.received = System.nanoTime();
    }
    if (ackDelivered(VoltronEntityMessage.Acks.COMPLETED)) {
      this.complete = System.nanoTime();
      if (pendingAcks.isEmpty()) {
        notifyAll();
      }

      if (error != null) {
        Assert.assertNull(value);
        this.pendingAcks.clear();
        this.exception = error;
        this.getCanComplete = true;
        if (asyncMode) {
          handleException(this.exception);
        }
        notifyAll();
      } else {
        Assert.assertNotNull(value);
        this.value = value;
        if (!this.blockGetOnRetired) {
          this.getCanComplete = true;
          if (asyncMode) {
            handleMessage(value);
          }
          notifyAll();
        }
      }
    }
  }
  
  synchronized void handleException(Exception ee) {
    if (monitor != null) {
      monitor.exception(ExceptionUtils.convert(ee));
    }
  }

  synchronized void handleMessage(byte[] raw) {
    if (monitor != null) {
      monitor.accept(raw);
    }
  }

  private boolean ackDelivered(VoltronEntityMessage.Acks ack) {
    if (Trace.isTraceEnabled()) {
      trace.log("Received ACK: " + ack);
    }
    if (this.pendingAcks.remove(ack) && monitor != null) {
      monitor.ackDelivered(ack);
    }
    return this.outstandingAcks.remove(ack);
  }
  
  private synchronized Runnable retiredBookeeping() {
    if (ackDelivered(VoltronEntityMessage.Acks.RETIRED)) {
      this.retired = System.nanoTime();
      if (this.blockGetOnRetired) {
        this.getCanComplete = true;
        if (message.getVoltronType() == VoltronEntityMessage.Type.INVOKE_ACTION) {
          Assert.assertTrue("failed " + this.message.getTransactionID(), value != null || exception != null);
        }
      }
      notifyAll();
      return this.runOnRetire;
    } else {
      return null;
    }
  }

  public void retired() {
    Runnable runAfter = retiredBookeeping();
    if (monitor != null) {
      monitor.close();
    }
    if (runAfter != null) {
      runAfter.run();
    }
  }
  
  void addServerStatistics(long[] stats) {
    this.serverStats = stats;
  }
}
