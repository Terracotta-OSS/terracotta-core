/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import java.util.Properties;
import org.terracotta.entity.ActiveInvokeChannel;
import org.terracotta.entity.ActiveInvokeContext;
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
  private final EntityMessage message;
  private final ClientDescriptor descriptor;
  private final IMessageSenderWrapper monitor;
  private final PassthroughRetirementManager retirement;

  public PassThroughServerActiveInvokeContext(M message, ClientDescriptor descriptor, int concurrencyKey, long current, long
    oldest, IMessageSenderWrapper monitor, PassthroughRetirementManager retirement, MessageCodec<M, R> codec) {
    super(descriptor == null ? null : descriptor.getSourceId(), concurrencyKey, current, oldest);
    this.message = message;
    this.descriptor = descriptor;
    this.monitor = monitor;
    this.retirement = retirement;
    this.codec = codec;
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
  public Properties getClientSourceProperties() {
    Properties props = new Properties();
    props.setProperty("clientID", String.valueOf(descriptor.getSourceId()));
    return props;
  }
}