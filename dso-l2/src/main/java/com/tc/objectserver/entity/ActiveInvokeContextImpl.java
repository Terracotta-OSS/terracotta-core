package com.tc.objectserver.entity;

import java.util.function.Consumer;
import org.terracotta.entity.ActiveInvokeChannel;
import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityResponse;

public class ActiveInvokeContextImpl<R extends EntityResponse> extends InvokeContextImpl implements ActiveInvokeContext<R> {
  private final ClientDescriptorImpl clientDescriptor;
  private final Consumer<R> messages;
  private final Consumer<Exception> exception;
  private final Runnable open;
  private final Runnable retire;
  
  public ActiveInvokeContextImpl(ClientDescriptorImpl descriptor, int concurrencyKey, long oldestid, long currentId) {
    this(descriptor, concurrencyKey, oldestid, currentId, null, null, null, null);
  }
  
  public ActiveInvokeContextImpl(ClientDescriptorImpl descriptor, int concurrencyKey, long oldestid, long currentId, 
      Runnable open, Consumer<R> messages, Consumer<Exception> exception, Runnable retire
  ) {
    super(new ClientSourceIdImpl(descriptor.getNodeID().toLong()), concurrencyKey, oldestid, currentId);
    this.clientDescriptor = descriptor;
    this.open = open;
    this.messages = messages;
    this.exception = exception;
    this.retire = retire;
  }

  @Override
  public ClientDescriptor getClientDescriptor() {
    return clientDescriptor;
  }

  @Override
  public ActiveInvokeChannel<R> openInvokeChannel() {
    if (open == null) {
      throw new UnsupportedOperationException("unable to create channel");
    } else {
      open.run();
      return new ActiveInvokeChannelImpl<>(messages, exception, retire);
    }
  }
}
