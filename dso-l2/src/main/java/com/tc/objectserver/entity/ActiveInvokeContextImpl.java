package com.tc.objectserver.entity;

import java.util.function.Consumer;
import org.terracotta.entity.ActiveInvokeChannel;
import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.exception.EntityException;

public class ActiveInvokeContextImpl extends InvokeContextImpl implements ActiveInvokeContext {
  private final ClientDescriptorImpl clientDescriptor;
  private final Consumer<byte[]> messages;
  private final Consumer<EntityException> exception;
  private final Runnable open;
  private final Runnable retire;
  
  public ActiveInvokeContextImpl(ClientDescriptorImpl descriptor, int concurrencyKey, long oldestid, long currentId) {
    this(descriptor, concurrencyKey, oldestid, currentId, null, null, null, null);
  }
  
  public ActiveInvokeContextImpl(ClientDescriptorImpl descriptor, int concurrencyKey, long oldestid, long currentId, 
      Runnable open, Consumer<byte[]> messages, Consumer<EntityException> exception, Runnable retire
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
  public ActiveInvokeChannel openInvokeChannel() {
    if (open == null) {
      throw new UnsupportedOperationException("unable to create channel");
    } else {
      open.run();
      return new ActiveInvokeChannelImpl(messages, exception, retire);
    }
  }
}
