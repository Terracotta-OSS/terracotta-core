package org.terracotta.entity;

import java.util.Properties;

public class PassThroughEntityActiveInvokeContext<R extends EntityResponse> extends PassThroughEntityInvokeContext implements
  ActiveInvokeContext<R> {
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

  @Override
  public ActiveInvokeChannel<R> openInvokeChannel() {
    return new PassThroughEntityActiveInvokeChannel<>();
  }

  @Override
  public Properties getClientSourceProperties() {
    Properties props = new Properties();
    props.setProperty("clientID", String.valueOf(descriptor.getSourceId()));
    return props;
  }
}
