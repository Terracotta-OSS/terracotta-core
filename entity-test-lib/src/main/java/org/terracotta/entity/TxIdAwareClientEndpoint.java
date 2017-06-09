package org.terracotta.entity;

/**
 * This is here as a seam for testing.
 *
 * @param <M>
 * @param <R>
 */
public interface TxIdAwareClientEndpoint<M extends EntityMessage, R extends EntityResponse>
  extends EntityClientEndpoint<M, R> {

  long getCurrentId();

  long resetEldestId();
}