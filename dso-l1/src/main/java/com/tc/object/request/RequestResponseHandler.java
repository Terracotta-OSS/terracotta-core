package com.tc.object.request;

import org.terracotta.exception.EntityException;
import com.tc.object.tx.TransactionID;


/**
 * Note that the methods in this interface can be called on multiple threads, concurrently, so the implementation must
 * synchronize where appropriate.
 */
public interface RequestResponseHandler {
  /**
   * Called when the server sends back the RECEIVED acknowledgement.
   */
  void received(TransactionID id);

  /**
   * Called when the server sends back a response that the request completed successfully, with no return value.  This
   * implies an APPLIED acknowledgement.
   */
  void complete(TransactionID id);

  /**
   * Called when the server sends back a response that the request completed successfully, with a return value.  This
   * implies an APPLIED acknowledgement.
   */
  void complete(TransactionID id, byte[] value);

  /**
   * Called when the server sends back a response that the request completed with a failure, as described by the
   * exception.  This implies an APPLIED acknowledgement.  Note that all of our wire-level exceptions are now
   * EntityException instances.
   */
  void failed(TransactionID id, EntityException e);
}
