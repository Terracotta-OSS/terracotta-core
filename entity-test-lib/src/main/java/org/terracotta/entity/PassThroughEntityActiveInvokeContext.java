package org.terracotta.entity;

public class PassThroughEntityActiveInvokeContext extends PassThroughEntityInvokeContext implements
  ActiveInvokeContext {
  private final ClientDescriptor descriptor;

  public PassThroughEntityActiveInvokeContext(ClientDescriptor descriptor, int concurrencyKey, long current, long
    oldest) {
    super(descriptor.getSourceId(), concurrencyKey, current, oldest);
    this.descriptor = descriptor;
  }

  @Override
  public ClientDescriptor getClientDescriptor() {
    return descriptor;
  }

}
