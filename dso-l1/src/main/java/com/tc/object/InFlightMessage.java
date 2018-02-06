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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * This is essentially a wrapper over an in-flight VoltronEntityMessage, used for tracking its response.
 * The message is stored here, since it is sent asynchronously, along with storage for the return value.
 * Note that this is only used from within ClietEntityManagerImpl, and was originally embedded there, but was extracted to
 * make unit testing more direct.
 */
public class InFlightMessage {
  private final VoltronEntityMessage message;
  private final EntityID eid;
  private final InFlightMonitor monitor;
  private final boolean isDeferred;
  /**
   * The set of pending ACKs determines when the caller returns from the send, in order to preserve ordering in the
   * client code.  This is different from being "done" which specifically means that the COMPLETED has happened,
   * potentially returning a value or exception.
   * ACKs are removed from this pending set, as they arrive.
   */
  private final Set<VoltronEntityMessage.Acks> pendingAcks;
  // Note that the point where we wait for acks isn't exposed outside the InvokeFuture interface so this set of waiting
  // threads only applies to those threads waiting to get a response.
  private final Set<Thread> waitingThreads;

  private boolean isSent;
  private EntityException exception;
  private byte[] value;
  private boolean canSetResult;
  private boolean getCanComplete;
  private final boolean blockGetOnRetired;
  private final Trace trace;
  
  public InFlightMessage(EntityID extraInfo, VoltronEntityMessage message, Set<VoltronEntityMessage.Acks> acks, InFlightMonitor monitor, boolean shouldBlockGetOnRetire, boolean isDeferred) {
    this.message = message;
    this.eid = extraInfo;
    this.monitor = monitor;
    Assert.assertNotNull(eid);
    Assert.assertNotNull(message);
    this.pendingAcks = EnumSet.noneOf(VoltronEntityMessage.Acks.class);
    this.pendingAcks.addAll(acks);
    this.waitingThreads = new HashSet<Thread>();
    this.blockGetOnRetired = shouldBlockGetOnRetire || isDeferred;
    // We always assume that we can set the result, the first time.
    this.canSetResult = true;
    this.trace = Trace.newTrace(message, "InFlightMessage");
    this.isDeferred = isDeferred;
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
    return ((TCMessage)this.message).send();
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
    ackDelivered(VoltronEntityMessage.Acks.SENT);
    if (this.pendingAcks.isEmpty()) {
      notifyAll();
    }
  }

  public synchronized void received() {
    ackDelivered(VoltronEntityMessage.Acks.RECEIVED);
    if (this.pendingAcks.isEmpty()) {
      notifyAll();
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
    timedWait(() -> getCanComplete, timeout, unit);
    if (exception != null) {
      throw ExceptionUtils.addLocalStackTraceToEntityException(eid, exception);
    } else {
      if (this.message.getVoltronType() == VoltronEntityMessage.Type.INVOKE_ACTION) {
        Assert.assertNotNull(value);
      }
      return value;
    }
  }

  public synchronized void setResult(byte[] value, EntityException error) {
    if (Trace.isTraceEnabled()) {
      trace.log("Received Result: " + value + " ; Exception: " + (error != null ? error.getLocalizedMessage() : "None"));
    }
    ackDelivered(VoltronEntityMessage.Acks.RECEIVED);
    ackDelivered(VoltronEntityMessage.Acks.COMPLETED);
    if (pendingAcks.isEmpty()) {
      notifyAll();
    }

    if (error != null) {
      Assert.assertNull(value);
      this.pendingAcks.clear();
      this.exception = error;
      this.getCanComplete = true;
      notifyAll();
    } else {
      Assert.assertNotNull(value);
      if (isDeferred) {
        pushOneMessage(value);
      } else if (this.canSetResult) {
        this.value = value;
        if (!this.blockGetOnRetired) {
          this.getCanComplete = true;
          notifyAll();
        }
        // Determine if this can be over-written - only if we are waiting for the retired.
        this.canSetResult = this.blockGetOnRetired;
      }
    }
  }
  
  
  
  private void pushOneMessage(byte[] raw) {
    if (isDeferred) {
      if (value != null && monitor != null) {
        monitor.accept(value);
      } 
    }
    value = raw;
  }
  
  public synchronized void handleMessage(byte[] raw) {
    pushOneMessage(raw);
  }
  
  private void ackDelivered(VoltronEntityMessage.Acks ack) {
    if (Trace.isTraceEnabled()) {
      trace.log("Received ACK: " + ack);
    }
    if (this.pendingAcks.remove(ack) && monitor != null) {
      monitor.ackDelivered(ack);
    }
  }

  public synchronized void retired() {
    ackDelivered(VoltronEntityMessage.Acks.RETIRED);
    if (this.blockGetOnRetired) {
      this.getCanComplete = true;
      if (message.getVoltronType() == VoltronEntityMessage.Type.INVOKE_ACTION) {
        Assert.assertTrue("failed " + this.message.getTransactionID(), value != null || exception != null);
      }
    }
    notifyAll();
    if (monitor != null) {
      monitor.close();
    }
  }
}
