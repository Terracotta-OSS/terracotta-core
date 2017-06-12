package org.terracotta.entity;

public class PassThroughEntityInvokeContext implements InvokeContext {
  private final ClientDescriptor descriptor;
  private final long current;
  private final long oldest;

  public PassThroughEntityInvokeContext(ClientDescriptor descriptor, long current, long oldest) {
    this.descriptor = descriptor;
    this.current = current;
    this.oldest = oldest;
  }

  @Override
  public ClientDescriptor getClientDescriptor() {
    return descriptor;
  }

  @Override
  public long getCurrentTransactionId() {
    return current;
  }

  @Override
  public long getOldestTransactionId() {
    return oldest;
  }
}
