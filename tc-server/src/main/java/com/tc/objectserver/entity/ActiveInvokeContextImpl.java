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
import java.util.Properties;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.ActiveEntityManager;
import org.terracotta.entity.ActiveInvokeChannel;
import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ActiveMessenger;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodecException;

public class ActiveInvokeContextImpl<M extends EntityMessage, R extends EntityResponse> extends InvokeContextImpl implements ActiveInvokeContext<M, R> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveInvokeContextImpl.class);

  private final M requestContext;
  private final ClientDescriptorImpl clientDescriptor;
  private final Consumer<EntityResponse> messages;
  private final Consumer<Exception> exception;
  private final Runnable open;
  private final Runnable retire;
  private final EntityMessengerService<M, R> messenger;
  private final Properties properties = GuardianContext.getCurrentChannelProperties();
  private ActiveInvokeChannelImpl channel;

  public ActiveInvokeContextImpl(ClientDescriptorImpl descriptor, int concurrencyKey, long oldestid, long currentId) {
    this(null, descriptor, concurrencyKey, oldestid, currentId, null, null, null, null, null);
  }

  public ActiveInvokeContextImpl(M request, ClientDescriptorImpl descriptor, int concurrencyKey, long oldestid, long currentId,
      Runnable open, Consumer<EntityResponse> messages, Consumer<Exception> exception, Runnable retire, EntityMessengerService<M, R> messenger
  ) {
    super(new ClientSourceIdImpl(descriptor.getNodeID().toLong()), concurrencyKey, oldestid, currentId);
    this.requestContext = request;
    this.clientDescriptor = descriptor;
    this.open = open;
    this.messages = messages;
    this.exception = exception;
    this.retire = retire;
    this.messenger = messenger;
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

  @Override
  public ActiveMessenger<M, R> createInvokeMessenger() {
    return new ActiveMessenger<M, R>() {
      @Override
      public void sendMessage(M message) {
        try {
          messenger.messageSelfAndDeferRetirement(requestContext, message);
        } catch (MessageCodecException codec) {
          LOGGER.warn("error encoding message", codec);
        }
      }

      @Override
      public void sendMessage(M message, Consumer<R> result, Consumer<Exception> failure) {
        try {
          messenger.messageSelfAndDeferRetirement(requestContext, message, (MessageResponse<R> t) -> {
            if (t.wasExceptionThrown()) {
              failure.accept(t.getException());
            } else {
              result.accept(t.getResponse());
            }
          });
        } catch (MessageCodecException codec) {
          failure.accept(codec);
        }
      }

      @Override
      public ActiveMessenger.ReleaseHandle deferRetirement(String tag, M message) {
        return new ActiveMessenger.ReleaseHandle() {
          @Override
          public String tag() {
            return tag;
          }

          @Override
          public void release() {
            try {
              messenger.messageSelfAndDeferRetirement(requestContext, message);
            } catch (MessageCodecException codec) {
              LOGGER.warn("error encoding message", codec);
            }
          }
        };
      }

      @Override
      public ActiveMessenger.ReleaseHandle deferRetirement(String tag, M message, Consumer<R> result, Consumer<Exception> failure) {
        return new ActiveMessenger.ReleaseHandle() {
          @Override
          public String tag() {
            return tag;
          }

          @Override
          public void release() {
            try {
              messenger.messageSelfAndDeferRetirement(requestContext, message, (MessageResponse<R> t) -> {
                if (t.wasExceptionThrown()) {
                  failure.accept(t.getException());
                } else {
                  result.accept(t.getResponse());
                }
              });
            } catch (MessageCodecException codec) {
              failure.accept(codec);
            }
          }
        };
      }

      @Override
      public void close() {

      }
    };
  }

  @Override
  public ActiveEntityManager createEntityManager() {
    return new ActiveEntityManager() {
      @Override
      public void create(String type, String name, long version, byte[] configuration) {
        messenger.create(type, name, version, configuration);
      }

      @Override
      public void destroySelf() {
        messenger.destroySelf();
      }

      @Override
      public void reconfigureSelf(byte[] configuration) {
        messenger.reconfigureSelf(configuration);
      }

      @Override
      public void close() {

      }
    };
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
    return properties;
  }
}
