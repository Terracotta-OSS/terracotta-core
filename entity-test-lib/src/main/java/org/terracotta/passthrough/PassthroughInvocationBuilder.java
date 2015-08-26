package org.terracotta.passthrough;

import java.util.concurrent.Future;

import org.terracotta.entity.InvocationBuilder;


public class PassthroughInvocationBuilder implements InvocationBuilder {
  private final PassthroughConnection connection;
  private final Class<?> entityClass;
  private final String entityName;
  private final long clientInstanceID;
  
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
  public Future<byte[]> invoke() {
    return this.connection.invokeActionAndWaitForAcks(this.entityClass, this.entityName, this.clientInstanceID, this.shouldWaitForReceived, this.shouldWaitForCompleted, this.shouldReplicate, this.payload);
  }

}
