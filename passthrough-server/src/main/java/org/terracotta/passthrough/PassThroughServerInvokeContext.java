package org.terracotta.passthrough;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.InvokeContext;

public class PassThroughServerInvokeContext implements InvokeContext {
  private final ClientDescriptor descriptor;
  private final long current;
  private final long oldest;

  public PassThroughServerInvokeContext(ClientDescriptor descriptor, long current, long oldest) {
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
