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

  private boolean isSent;
  private EntityException exception;
  private byte[] value;
  private boolean done;

  public InFlightMessage(NetworkVoltronEntityMessage message, Set<VoltronEntityMessage.Acks> acks) {
    this.message = message;
    this.pendingAcks = EnumSet.noneOf(VoltronEntityMessage.Acks.class);
    this.pendingAcks.addAll(acks);
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
  public void interrupt() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public synchronized boolean isDone() {
    return done;
  }

  @Override
  public synchronized byte[] get() throws InterruptedException, EntityException {
    while (!done) {
      wait();
    }
    if (exception != null) {
      throw this.exception;
    } else {
      return value;
    }
  }

  @Override
  public synchronized byte[] getWithTimeout(long timeout, TimeUnit unit) throws InterruptedException, EntityException, TimeoutException {
    long end = System.nanoTime() + unit.toNanos(timeout);
    while (!done) {
      long timing = end - System.nanoTime();
      if (timing <= 0) {
        throw new TimeoutException();
      } else {
        wait(timing / TimeUnit.MILLISECONDS.toNanos(1), (int)(timing % TimeUnit.MILLISECONDS.toNanos(1))); 
      }
    }
    if (exception != null) {
      throw this.exception;
    } else {
      return value;
    }
  }

  synchronized void setResult(byte[] value, EntityException e) {
    this.pendingAcks.remove(VoltronEntityMessage.Acks.APPLIED);
    this.exception = e;
    this.value = value;
    this.done = true;
    notifyAll();
  }
}
