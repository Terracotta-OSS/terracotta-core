package org.terracotta.passthrough;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class PassthroughWait implements Future<byte[]> {
  private boolean waitingForReceive;
  private boolean waitingForComplete;
  private boolean didComplete;
  private byte[] response;
  private Exception error;
  
  public PassthroughWait(boolean shouldWaitForReceived, boolean shouldWaitForCompleted) {
    this.waitingForReceive = shouldWaitForReceived;
    this.waitingForComplete = shouldWaitForCompleted;
    this.didComplete = false;
    this.response = null;
    this.error = null;
  }
  
  public synchronized void waitForAck() {
    while (this.waitingForReceive || this.waitingForComplete) {
      try {
        wait();
      } catch (InterruptedException e) {
        Assert.fail(e);
      }
    }
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    throw new IllegalStateException("Not supported");
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public synchronized boolean isDone() {
    return this.didComplete;
  }

  @Override
  public synchronized byte[] get() throws InterruptedException, ExecutionException {
    while (!this.didComplete) {
      this.wait();
    }
    if (null != this.error) {
      throw new ExecutionException(this.error);
    }
    return this.response;
  }

  @Override
  public byte[] get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    throw new IllegalStateException("Not supported");
  }

  public synchronized void handleAck() {
    this.waitingForReceive = false;
    notifyAll();
  }

  public synchronized void handleComplete(byte[] result, Exception error) {
    this.waitingForComplete = false;
    this.didComplete = true;
    this.response = result;
    this.error = error;
    notifyAll();
  }
}
