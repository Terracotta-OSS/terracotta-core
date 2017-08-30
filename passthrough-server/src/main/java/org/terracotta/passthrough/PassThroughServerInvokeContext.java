package org.terracotta.passthrough;

import org.terracotta.entity.ClientSourceId;
import org.terracotta.entity.InvokeContext;

public class PassThroughServerInvokeContext implements InvokeContext {
  private final long current;
  private final long oldest;
  private final ClientSourceId sourceId;

  public PassThroughServerInvokeContext(ClientSourceId sourceId, long current, long oldest) {
    this.sourceId = sourceId;
    this.current = current;
    this.oldest = oldest;
  }

  @Override
  public ClientSourceId getClientSource() {
    return sourceId;
  }

  @Override
  public long getCurrentTransactionId() {
    return current;
  }

  @Override
  public long getOldestTransactionId() {
    return oldest;
  }

  @Override
  public boolean isValidClientInformation() {
    return current > 0;
  }

  @Override
  public ClientSourceId makeClientSourceId(long l) {
    return new PassthroughClientSourceId(l);
  }

}
