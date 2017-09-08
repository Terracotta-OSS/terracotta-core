package org.terracotta.passthrough;

import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ClientDescriptor;

public class PassThroughServerActiveInvokeContext extends PassThroughServerInvokeContext
  implements ActiveInvokeContext {
  private final ClientDescriptor descriptor;

  public PassThroughServerActiveInvokeContext(ClientDescriptor descriptor, int concurrencyKey, long current, long
    oldest) {
    super(descriptor == null ? null : descriptor.getSourceId(), concurrencyKey, current, oldest);
    this.descriptor = descriptor;
  }

  @Override
  public ClientDescriptor getClientDescriptor() {
    return descriptor;
  }

}