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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import org.terracotta.entity.InvocationBuilder;
import org.terracotta.entity.InvokeFuture;


/**
 * Used by the client-side PassthroughEntityClientEndpoint to build the invocation which will be sent to the server.
 * Note that this isn't where the invocation ack is tracked, just the object which builds that message and ack tracking
 * mechanism (by requesting it in the underlying PassthroughConnection).
 */
public class PassthroughInvocationBuilder implements InvocationBuilder {
  private final PassthroughConnection connection;
  private final Class<?> entityClass;
  private final String entityName;
  private final long clientInstanceID;
  
  private boolean shouldWaitForSent;
  private boolean shouldWaitForReceived;
  private boolean shouldWaitForCompleted;
  private boolean shouldReplicate;
  private byte[] payload;
  
  public PassthroughInvocationBuilder(PassthroughConnection connection, Class<?> entityClass, String entityName, long clientInstanceID) {
    this.connection = connection;
    this.entityClass = entityClass;
    this.entityName = entityName;
    this.clientInstanceID = clientInstanceID;
  }

  @Override
  public InvocationBuilder ackSent() {
    this.shouldWaitForSent = true;
    return this;
  }

  @Override
  public InvocationBuilder ackReceived() {
    this.shouldWaitForReceived = true;
    return this;
  }

  @Override
  public InvocationBuilder ackCompleted() {
    this.shouldWaitForCompleted = true;
    return this;
  }

  @Override
  public InvocationBuilder replicate(boolean requiresReplication) {
    this.shouldReplicate = requiresReplication;
    return this;
  }

  @Override
  public InvocationBuilder payload(byte[] payload) {
    this.payload = payload;
    return this;
  }

  @Override
  public InvokeFuture<byte[]> invoke() {
    PassthroughMessage message = PassthroughMessageCodec.createInvokeMessage(this.entityClass, this.entityName, this.clientInstanceID, this.payload, this.shouldReplicate);
    return this.connection.invokeActionAndWaitForAcks(message, this.shouldWaitForSent, this.shouldWaitForReceived, this.shouldWaitForCompleted);
  }

}
