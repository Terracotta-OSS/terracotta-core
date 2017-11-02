package org.terracotta.entity;

public class PassThroughEntityActiveInvokeContext<R extends EntityResponse> extends PassThroughEntityInvokeContext implements
  ActiveInvokeContext<R> {
  private final ClientDescriptor descriptor;
  private final InvokeMonitor<R> monitor;

  public PassThroughEntityActiveInvokeContext(ClientDescriptor descriptor, int concurrencyKey, long current, long
    oldest, InvokeMonitor<R> monitor) {
    super(descriptor.getSourceId(), concurrencyKey, current, oldest);
    this.descriptor = descriptor;
    this.monitor = monitor;
  }

  @Override
  public ClientDescriptor getClientDescriptor() {
    return descriptor;
  }

  @Override
  public ActiveInvokeChannel<R> openInvokeChannel() {
    return new PassThroughEntityActiveInvokeChannel<>(monitor);
  }

}
