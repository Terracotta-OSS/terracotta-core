/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.entity;

import com.tc.objectserver.core.impl.GuardianContext;
import com.tc.services.EntityMessengerService;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.ActiveInvokeChannel;
import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.ActiveServerMessenger;

public class ActiveInvokeContextImpl<R extends EntityResponse> extends InvokeContextImpl implements ActiveInvokeContext<R> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveInvokeContextImpl.class);

  private final EntityMessage requestContext;
  private final ClientDescriptorImpl clientDescriptor;
  private final Supplier<ActiveInvokeChannel> channelCreate;
  private final EntityMessengerService<EntityMessage, EntityResponse> messenger;
  private final Properties properties = GuardianContext.getCurrentChannelProperties();

  private RefCountingActiveInvokeChannel<R> channel = null;

  public ActiveInvokeContextImpl(EntityMessage request, ClientDescriptorImpl descriptor, int concurrencyKey, long oldestid, long currentId,
    Supplier<ActiveInvokeChannel> channelCreate, EntityMessengerService<EntityMessage, EntityResponse> messenger
  ) {
    super(new ClientSourceIdImpl(descriptor.getNodeID().toLong()), concurrencyKey, oldestid, currentId);
    this.requestContext = Objects.requireNonNull(request);
    this.clientDescriptor = Objects.requireNonNull(descriptor);
    this.channelCreate = channelCreate;
    this.messenger = messenger;
  }

  @Override
  public ClientDescriptor getClientDescriptor() {
    return clientDescriptor;
  }

  @Override
  public ActiveInvokeChannel<R> openInvokeChannel() {
    if (channelCreate == null) {
      throw new UnsupportedOperationException("unable to create channel");
    } else {
      return getOrCreateInvokeChannel();
    }
  }

  @Override
  public ActiveServerMessenger createServerMessenger() {
    return new ActiveServerMessenger() {
      @Override
      public void sendMessage(EntityMessage message) {
        sendMessage(message, null, null);
      }

      @Override
      public void sendMessage(EntityMessage message, Consumer<EntityResponse> result, Consumer<Exception> failure) {
        try {
          if (message == requestContext) {
            throw new AssertionError("message being sent is the same as the parent request.  Messages cnnot be scheduled twice");
          }
          messenger.messageSelfAndDeferRetirement(requestContext, message, t -> {
            if (t.wasExceptionThrown()) {
              if (failure != null) {
                failure.accept(t.getException());
              }
            } else {
              if (result != null) {
                result.accept(t.getResponse());
              }
            }
          });
        } catch (MessageCodecException codec) {
          if (failure != null) {
            failure.accept(codec);
          }
        }
      }

      @Override
      public ActiveServerMessenger.ReleaseHandle deferRetirement(String tag, EntityMessage message) {
        return deferRetirement(tag, message, null, null);
      }

      @Override
      public ActiveServerMessenger.ReleaseHandle deferRetirement(String tag, EntityMessage message, Consumer<EntityResponse> result, Consumer<Exception> failure) {
        if (message == requestContext) {
          throw new AssertionError("message being sent is the same as the parent request.  Messages cnnot be scheduled twice");
        }
        EntityMessengerService.Handle handle = messenger.deferRetirement(tag, requestContext, message);
        return new ActiveServerMessenger.ReleaseHandle() {
          @Override
          public String tag() {
            return tag;
          }

          @Override
          public void release() {
            handle.release(result, failure);
          }
        };
      }

      @Override
      public void close() {

      }
    };
  }

  private synchronized ActiveInvokeChannel<R> getOrCreateInvokeChannel() {
    // this is an optimization to not grind the retirement manager with a bunch of
    // open and closes
    if (channel == null || channel.reference() == 0) {
      channel = new RefCountingActiveInvokeChannel<>(channelCreate.get());
    }
    return new CloseableActiveInvokeChannel<>(channel);
  }

  @Override
  public Properties getClientSourceProperties() {
    return properties;
  }
}
