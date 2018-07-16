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
package com.tc.objectserver.entity;

import com.tc.objectserver.api.ResultCapture;
import com.tc.tracing.Trace;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.exception.EntityException;

/**
 *
 */
 
public class ResultCaptureImpl implements ResultCapture {
  private final Runnable received;
  private final Consumer<byte[]> result;
  private final Consumer<byte[]> message;
  private final Consumer<EntityException> error;
  private final SetOnceFlag receivedSent = new SetOnceFlag();
  private static final Logger LOGGER = LoggerFactory.getLogger(ResultCaptureImpl.class);

  Supplier<ActivePassiveAckWaiter> setOnce;
  
  public ResultCaptureImpl(Runnable received, Consumer<byte[]> result, Consumer<byte[]> message, Consumer<EntityException> error) {
    this.received = received;
    this.result = result;
    this.error = error;
    this.message = message;
  }

  @Override
  public void setWaitFor(Supplier<ActivePassiveAckWaiter> waitFor) {
    Assert.assertNull(setOnce);
    setOnce = waitFor;
  }

  @Override
  public void waitForReceived() {
    if (setOnce != null) {
      ActivePassiveAckWaiter waiter = setOnce.get();
      waiter.waitForReceived();
    }
  }

  @Override
  public void received() {
    Trace.activeTrace().log("received ");
    this.receivedSent.set();
    if (received != null) {
      received.run();
    }
  }

  @Override
  public void complete() {
    Trace.activeTrace().log("Completed without result ");
    if (!this.receivedSent.isSet()) {
      received();
    }
    if (result != null) {
      result.accept(null);
    }
  }  

  @Override
  public void complete(byte[] value) {
    if (Trace.isTraceEnabled()) {
      Trace.activeTrace().log("Completed with result: " + value);
    }
    if (!this.receivedSent.isSet()) {
      received();
    }
    if (result != null) {
      result.accept(value);
    }
  }

  @Override
  public void failure(EntityException ee) {
    if (Trace.isTraceEnabled()) {
      Trace.activeTrace().log("Failure - exception: " + ee.getLocalizedMessage());
    }
    if (!this.receivedSent.isSet()) {
      received();
    }
    if (error != null) {
      error.accept(ee);
    }
  }
  
  @Override
  public void message(byte[] m) {
    message.accept(m);
  }

  @Override
  public void retired() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  
  
}
