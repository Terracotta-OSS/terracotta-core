package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.tx.ServerTransactionID;

/**
 * @author tim
 */
public class ServerTransactionCompleteContext implements EventContext {
  private final ServerTransactionID stxID;

  public ServerTransactionCompleteContext(final ServerTransactionID stxID) {
    this.stxID = stxID;
  }

  public ServerTransactionID getServerTransactionID() {
    return stxID;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ServerTransactionCompleteContext that = (ServerTransactionCompleteContext) o;

    if (!stxID.equals(that.stxID)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return stxID.hashCode();
  }
}
