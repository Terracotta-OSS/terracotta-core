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

import org.terracotta.entity.InvokeFuture;
import org.terracotta.exception.EntityException;

import com.tc.entity.NetworkVoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * This is essentially a wrapper over an in-flight VoltronEntityMessage, used for tracking its response.
 * The message is stored here, since it is sent asynchronously, along with storage for the return value.
 * Note that this is only used from within ClietEntityManagerImpl, and was originally embedded there, but was extracted to
 * make unit testing more direct.
 */
public class InFlightMessage implements InvokeFuture<byte[]> {
  private final NetworkVoltronEntityMessage message;
  /**
   * The set of pending ACKs determines when the caller returns from the send, in order to preserve ordering in the
   * client code.  This is different from being "done" which specifically means that the APPLIED has happened,
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

  public InFlightMessage(NetworkVoltronEntityMessage message, Set<VoltronEntityMessage.Acks> acks, boolean shouldBlockGetOnRetire) {
    this.message = message;
    this.pendingAcks = EnumSet.noneOf(VoltronEntityMessage.Acks.class);
    this.pendingAcks.addAll(acks);
    this.waitingThreads = new HashSet<Thread>();
    this.blockGetOnRetired = shouldBlockGetOnRetire;
    
    // We always assume that we can set the result, the first time.
    this.canSetResult = true;
  }

  /**
   * Used when populating the reconnect handshake.
   */
  public NetworkVoltronEntityMessage getMessage() {
    return this.message;
  }

  public TransactionID getTransactionID() {
    return this.message.getTransactionID();
  }

  public void send() {
    Assert.assertFalse(this.isSent);
    this.isSent = true;
    this.message.send();
  }

  public synchronized void waitForAcks() {
    boolean interrupted = false;
    while (!this.pendingAcks.isEmpty()) {
      try {
        wait();
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  public synchronized void sent() {
    if (this.pendingAcks.remove(VoltronEntityMessage.Acks.SENT)) {
      if (this.pendingAcks.isEmpty()) {
        notifyAll();
      }
    }
  }

  public synchronized void received() {
    if (this.pendingAcks.remove(VoltronEntityMessage.Acks.RECEIVED)) {
      if (this.pendingAcks.isEmpty()) {
        notifyAll();
      }
    }
  }

  @Override
  public synchronized void interrupt() {
    for (Thread waitingThread : this.waitingThreads) {
      waitingThread.interrupt();
    }
  }

  @Override
  public synchronized boolean isDone() {
    return this.getCanComplete;
  }

  @Override
  public synchronized byte[] get() throws InterruptedException, EntityException {
    Thread callingThread = Thread.currentThread();
    boolean didAdd = this.waitingThreads.add(callingThread);
    // We can't have already been waiting.
    Assert.assertTrue(didAdd);
    
    try {
      while (!this.getCanComplete) {
        wait();
      }
    } finally {
      // We will hit this path on interrupt, for example.
      this.waitingThreads.remove(callingThread);
    }
    
    // If we didn't throw due to interruption, we fall through here.
    if (exception != null) {
      throw this.exception;
    } else {
      return value;
    }
  }

  @Override
  public synchronized byte[] getWithTimeout(long timeout, TimeUnit unit) throws InterruptedException, EntityException, TimeoutException {
    Thread callingThread = Thread.currentThread();
    boolean didAdd = this.waitingThreads.add(callingThread);
    // We can't have already been waiting.
    Assert.assertTrue(didAdd);
    
    long end = System.nanoTime() + unit.toNanos(timeout);
    try {
      while (!this.getCanComplete) {
        long timing = end - System.nanoTime();
        if (timing <= 0) {
          throw new TimeoutException();
        } else {
          wait(timing / TimeUnit.MILLISECONDS.toNanos(1), (int)(timing % TimeUnit.MILLISECONDS.toNanos(1))); 
        }
      }
    } finally {
      this.waitingThreads.remove(callingThread);
    }
    if (exception != null) {
      throw this.exception;
    } else {
      return value;
    }
  }

  synchronized void setResult(byte[] value, EntityException error) {
    this.pendingAcks.remove(VoltronEntityMessage.Acks.APPLIED);
    if (this.canSetResult) {
      this.exception = error;
      this.value = value;
      if (!this.blockGetOnRetired) {
        this.getCanComplete = true;
        notifyAll();
      }
      // Determine if this can be over-written - only if we are waiting for the retired.
      this.canSetResult = this.blockGetOnRetired;
    }
  }

  public synchronized void retired() {
    this.pendingAcks.remove(VoltronEntityMessage.Acks.RETIRED);
    if (this.blockGetOnRetired) {
      this.getCanComplete = true;
      notifyAll();
    }
  }
}
