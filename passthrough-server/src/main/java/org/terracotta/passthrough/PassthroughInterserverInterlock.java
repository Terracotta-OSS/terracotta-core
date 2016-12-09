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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import org.terracotta.exception.EntityException;

/**
 * In the case where we are an active sending a message to a downstream passive, we use this implementation to provide the
 * basic interlock across the 2 threads.
 */
public class PassthroughInterserverInterlock implements IMessageSenderWrapper {
  private final IMessageSenderWrapper sender;
  private boolean isComplete = false;
  private boolean didSucceed = false;
  private boolean isRetired = false;
  
  public PassthroughInterserverInterlock(IMessageSenderWrapper sender) {
    this.sender = sender;
  }

  public synchronized boolean waitForComplete() {
    while (!this.isComplete) {
      try {
        wait();
      } catch (InterruptedException e) {
        Assert.unexpected(e);
      }
    }
    return this.didSucceed;
  }

  public synchronized boolean waitForRetired() {
    while (!this.isRetired) {
      try {
        wait();
      } catch (InterruptedException e) {
        Assert.unexpected(e);
      }
    }
    return this.didSucceed;
  }
  @Override
  public void sendAck(PassthroughMessage ack) {
  }
  @Override
  public synchronized void sendComplete(PassthroughMessage complete, EntityException error) {
    this.isComplete = true;
    this.didSucceed = (null == error);
    notifyAll();
  }
  @Override
  public synchronized void sendRetire(PassthroughMessage retire) {
    this.isRetired = true;
    notifyAll();
  }
  @Override
  public PassthroughClientDescriptor clientDescriptorForID(long clientInstanceID) {
    Assert.unreachable();
    return null;
  }
  @Override
  public long getClientOriginID() {
    // Note that we will have a null sender when this is being used for sync messages.  Those are
    // internally-originating messages so return -1.
    long origin = -1;
    if (null != sender) {
      origin = sender.getClientOriginID();
    }
    return origin;
  }
}