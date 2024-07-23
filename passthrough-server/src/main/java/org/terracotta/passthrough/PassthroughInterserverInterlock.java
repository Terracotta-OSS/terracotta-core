/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.passthrough;

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
  public synchronized void sendComplete(PassthroughMessage complete, boolean last) {
    this.isComplete = last;
    this.didSucceed = (complete.type != PassthroughMessage.Type.MONITOR_EXCEPTION);
    notifyAll();
  }

  @Override
  public synchronized void sendRetire(PassthroughMessage retire) {
    this.isRetired = true;
    notifyAll();
  }

  @Override
  public PassthroughClientDescriptor clientDescriptorForID(long clientInstanceID) {
    return new PassthroughClientDescriptor(null, null, clientInstanceID);
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