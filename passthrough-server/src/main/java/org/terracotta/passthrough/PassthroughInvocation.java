/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import java.util.Set;

import org.terracotta.entity.Invocation;
import org.terracotta.entity.InvocationCallback;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;


/**
 * Used by the client-side PassthroughEntityClientEndpoint to build the invocation which will be sent to the server.
 * Note that this isn't where the invocation ack is tracked, just the object which builds that message and ack tracking
 * mechanism (by requesting it in the underlying PassthroughConnection).
 */
public class PassthroughInvocation<M extends EntityMessage, R extends EntityResponse> implements Invocation<R> {
  private final PassthroughConnection connection;
  private final String entityClassName;
  private final String entityName;
  private final long clientInstanceID;
  private final MessageCodec<M, R> messageCodec;
  private final M request;

  public PassthroughInvocation(PassthroughConnection connection, String entityClassName, String entityName, long clientInstanceID, MessageCodec<M, R> messageCodec, M request) {
    this.connection = connection;
    this.entityClassName = entityClassName;
    this.entityName = entityName;
    this.clientInstanceID = clientInstanceID;
    this.messageCodec = messageCodec;
    this.request = request;
  }

  @Override
  public Task invoke(InvocationCallback<R> callback, Set<InvocationCallback.Types> callbacks) {
    final PassthroughMessage message;
    try {
      message = PassthroughMessageCodec.createInvokeMessage(this.entityClassName, this.entityName, this.clientInstanceID, messageCodec.encodeMessage(this.request), true);
    } catch (MessageCodecException e) {
      callback.failure(e);
      callback.complete();
      callback.retired();
      return () -> false;
    }


    return connection.invoke(message, new InvocationCallback<byte[]>() {
      @Override
      public void sent() {
        callback.sent();
      }

      @Override
      public void received() {
        callback.received();
      }

      @Override
      public void result(byte[] response) {
        try {
          callback.result(messageCodec.decodeResponse(response));
        } catch (MessageCodecException e) {
          callback.failure(e);
        }
      }

      @Override
      public void failure(Throwable failure) {
        callback.failure(failure);
      }

      @Override
      public void complete() {
        callback.complete();
      }

      @Override
      public void retired() {
        callback.retired();
      }
    }, callbacks);
  }
}
