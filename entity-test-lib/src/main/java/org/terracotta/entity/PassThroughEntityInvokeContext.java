package org.terracotta.entity;

public class PassThroughEntityInvokeContext implements InvokeContext {
  private final long current;
  private final long oldest;
  private final ClientSourceId sourceId;
  private final int concurrencyKey;

  public PassThroughEntityInvokeContext(ClientSourceId sourceId, int concurrencyKey, long current, long oldest) {
    this.sourceId=sourceId;
    this.current = current;
    this.oldest = oldest;
    this.concurrencyKey = concurrencyKey;
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
  public int getConcurrencyKey() {
    return concurrencyKey;
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
