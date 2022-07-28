package org.terracotta.passthrough;

import org.terracotta.entity.InvocationCallback;

public class PassthroughInvocationCallback implements InvocationCallback<byte[]> {

  private final byte[] message;
  private final InvocationCallback<byte[]> callback;

  public PassthroughInvocationCallback(byte[] message, InvocationCallback<byte[]> callback) {
    this.message = message;
    this.callback = callback;
  }

  public void forceDisconnect() {

  }

  public byte[] getMessage() {
    return message;
  }

  @Override
  public void sent() {
    callback.sent();
  }

  @Override
  public void received() {
    callback.received();
  }

  @Override
  public void result(byte[] response) {
    callback.result(response);
  }

  @Override
  public void failure(Throwable failure) {
    callback.failure(failure);
  }

  @Override
  public void complete() {
    callback.complete();
  }

  @Override
  public void retired() {
    callback.retired();
  }
}
