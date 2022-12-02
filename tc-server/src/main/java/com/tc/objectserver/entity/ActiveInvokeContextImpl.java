/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.entity;

import com.tc.objectserver.core.impl.GuardianContext;
import java.util.Properties;
import java.util.function.Consumer;
import org.terracotta.entity.ActiveInvokeChannel;
import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityResponse;

public class ActiveInvokeContextImpl<R extends EntityResponse> extends InvokeContextImpl implements ActiveInvokeContext<R> {
  private final ClientDescriptorImpl clientDescriptor;
  private final Consumer<EntityResponse> messages;
  private final Consumer<Exception> exception;
  private final Runnable open;
  private final Runnable retire;
  private ActiveInvokeChannelImpl channel;
  
  public ActiveInvokeContextImpl(ClientDescriptorImpl descriptor, int concurrencyKey, long oldestid, long currentId) {
    this(descriptor, concurrencyKey, oldestid, currentId, null, null, null, null);
  }
  
  public ActiveInvokeContextImpl(ClientDescriptorImpl descriptor, int concurrencyKey, long oldestid, long currentId, 
      Runnable open, Consumer<EntityResponse> messages, Consumer<Exception> exception, Runnable retire
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
      return getOrCreateInvokeChannel();
    }
  }
  
  private synchronized ActiveInvokeChannel<R> getOrCreateInvokeChannel() {
    if (channel == null || !channel.reference()) {
      open.run();
      channel = new ActiveInvokeChannelImpl(messages, exception, retire);
    }
    return new CloseableActiveInvokeChannel<>(channel);
  }
  
  @Override
  public Properties getClientSourceProperties() {
    return GuardianContext.getCurrentChannelProperties();
  }
}
