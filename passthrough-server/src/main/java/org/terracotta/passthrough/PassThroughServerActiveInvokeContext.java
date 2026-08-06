/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.passthrough;

import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;
import org.terracotta.entity.ActiveInvokeChannel;
import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ActiveServerMessenger;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityServerException;

public class PassThroughServerActiveInvokeContext<M extends EntityMessage, R extends EntityResponse> extends PassThroughServerInvokeContext
  implements ActiveInvokeContext<R> {
  private final MessageCodec<M, R> codec;
  private final EntityMessage parent;
  private final PassthroughClientDescriptor descriptor;
  private final IMessageSenderWrapper monitor;
  private final PassthroughRetirementManager retirement;
  private final PassthroughServerProcess process;
  private final String entityClass;
  private final String entityName;

  public PassThroughServerActiveInvokeContext(M message, PassthroughClientDescriptor descriptor, int concurrencyKey, long current, long
    oldest, IMessageSenderWrapper monitor, PassthroughRetirementManager retirement, MessageCodec<M, R> codec, PassthroughServerProcess process, String entityClass, String entityName) {
    super(descriptor == null ? null : descriptor.getSourceId(), concurrencyKey, current, oldest);
    this.parent = Objects.requireNonNull(message);
    this.descriptor = Objects.requireNonNull(descriptor);
    this.monitor = monitor;
    this.retirement = retirement;
    this.codec = codec;
    this.process = process;
    this.entityClass = entityClass;
    this.entityName = entityName;
  }

  @Override
  public ClientDescriptor getClientDescriptor() {
    return descriptor;
  }

  @Override
  public ActiveInvokeChannel<R> openInvokeChannel() {
    monitor.open();
    return new ActiveInvokeChannel<R>() {
      @Override
      public void sendResponse(R response) {
        try {
          byte[] r = codec.encodeResponse(response);
          PassthroughMessage msg = PassthroughMessageCodec.createMonitorMessage(r, null);
          msg.setTransactionTracking(PassThroughServerActiveInvokeContext.this.getCurrentTransactionId(),PassThroughServerActiveInvokeContext.this.getOldestTransactionId());
          monitor.sendComplete(msg, false);
        } catch (MessageCodecException codec) {
          throw new RuntimeException(codec);
        }
      }

      @Override
      public void sendException(Exception excptn) {
        EntityException exp = (excptn instanceof EntityException) ? (EntityException)excptn : new EntityServerException(null, null, null, excptn);
        PassthroughMessage msg = PassthroughMessageCodec.createMonitorMessage(null, exp);
        msg.setTransactionTracking(PassThroughServerActiveInvokeContext.this.getCurrentTransactionId(), PassThroughServerActiveInvokeContext.this.getOldestTransactionId());
        monitor.sendComplete(msg, false);
      }

      @Override
      public void close() {
        monitor.close();
      }
    };
  }

  @Override
  public ActiveServerMessenger<R> createServerMessenger() {
    return new ActiveServerMessenger<>() {
      @Override
      public void sendMessage(EntityMessage message) {
        sendMessage(message, null);
      }

      @Override
      public void sendMessage(EntityMessage message, Consumer<Response<R>> result) {
        retirement.deferCurrentMessage(message);
        sendServerMessage(message, result);
      }

      @Override
      public ActiveServerMessenger.ReleaseHandle deferRetirement(String tag, EntityMessage message) {
        return deferRetirement(tag, message, null);
      }

      @Override
      public ActiveServerMessenger.ReleaseHandle deferRetirement(String tag, EntityMessage message, Consumer<Response<R>> result) {
        retirement.deferCurrentMessage(message);
        return new ReleaseHandle() {
          @Override
          public String tag() {
            return tag;
          }

          @Override
          public void release() {
            sendServerMessage(message, result);
          }
        };

      }

      @Override
      public void close() {

      }
    };
  }

  private void sendServerMessage(EntityMessage message, Consumer<ActiveServerMessenger.Response<R>> result) {
    process.sendMessageToActiveFromInsideActive(descriptor, message, makePassthroughMessage(message), m-> {
      try {
        if (result != null) {
          R response = codec.decodeResponse(m.asSerializedBytes());
          result.accept(new ActiveServerMessenger.Response<>() {

            @Override
            public R getResponse() throws Exception {
              return response;
            }
          });
        }
      } catch (MessageCodecException ce) {
        result.accept(new ActiveServerMessenger.Response<>() {
            @Override
            public R getResponse() throws Exception {
              throw ce;
            }
          });
      }
    });
  }

  private PassthroughMessage makePassthroughMessage(EntityMessage message) {
    @SuppressWarnings("unchecked")
    MessageCodec<EntityMessage, ?> codec = (MessageCodec<EntityMessage, ?>) this.codec;
    try {
      byte[] serializedMessage = codec.encodeMessage(message);
      long clientInstanceID = ((PassthroughClientDescriptor)descriptor).clientInstanceID;
      boolean shouldReplicateToPassives = true;
      PassthroughMessage passthroughMessage = PassthroughMessageCodec.createInvokeMessage(this.entityClass, this.entityName, clientInstanceID, serializedMessage, shouldReplicateToPassives);
      return passthroughMessage;
    } catch (MessageCodecException ce) {
      throw new RuntimeException(ce);
    }
  }

  @Override
  public Properties getClientSourceProperties() {
    Properties props = new Properties();
    props.setProperty("clientID", String.valueOf(descriptor.getSourceId()));
    return props;
  }
}