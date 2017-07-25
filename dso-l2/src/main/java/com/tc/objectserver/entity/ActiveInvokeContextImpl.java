package com.tc.objectserver.entity;

import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ClientDescriptor;

public class ActiveInvokeContextImpl extends InvokeContextImpl implements ActiveInvokeContext {
  private final ClientDescriptorImpl clientDescriptor;

  public ActiveInvokeContextImpl(ClientDescriptorImpl descriptor) {
    super(new ClientSourceIdImpl(descriptor.getNodeID().toLong()));
    this.clientDescriptor = descriptor;
  }

  public ActiveInvokeContextImpl(ClientDescriptorImpl descriptor, long oldestid, long currentId) {
    super(new ClientSourceIdImpl(descriptor.getNodeID().toLong()), oldestid, currentId);
    this.clientDescriptor = descriptor;
  }

  @Override
  public ClientDescriptor getClientDescriptor() {
    return clientDescriptor;
  }
}
