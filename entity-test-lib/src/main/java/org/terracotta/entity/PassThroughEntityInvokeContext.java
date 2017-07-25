package org.terracotta.entity;

public class PassThroughEntityInvokeContext implements InvokeContext {
  private final long current;
  private final long oldest;
  private final ClientSourceId sourceId;

  public PassThroughEntityInvokeContext(ClientSourceId sourceId, long current, long oldest) {
    this.sourceId=sourceId;
    this.current = current;
    this.oldest = oldest;
  }

  @Override
  public ClientSourceId getClientSource() {
    return sourceId;
  }

  @Override
  public ClientSourceId makeClientSourceId(long l) {
    // todo
    return null;
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
    return current >= 0;
  }
}
