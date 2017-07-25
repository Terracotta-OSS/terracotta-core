package com.tc.objectserver.entity;

import com.tc.object.tx.TransactionID;
import org.terracotta.entity.ClientSourceId;
import org.terracotta.entity.InvokeContext;

public class InvokeContextImpl implements InvokeContext {

  public static InvokeContext NULL_CONTEXT=new InvokeContextImpl();

  private final long oldestid;
  private final long currentId;
  private final ClientSourceId sourceId;

  private InvokeContextImpl() {
    this(ClientSourceIdImpl.NULL_ID, TransactionID.NULL_ID.toLong(), TransactionID.NULL_ID.toLong());
  }

  public InvokeContextImpl(ClientSourceId sourceid) {
    this(sourceid, TransactionID.NULL_ID.toLong(), TransactionID.NULL_ID.toLong());
  }

  public InvokeContextImpl(ClientSourceId sourceId, long oldestid, long currentId) {
    this.sourceId = sourceId;
    this.oldestid = oldestid;
    this.currentId = currentId;
  }

  @Override
  public ClientSourceId getClientSource() {
    return sourceId;
  }

  @Override
  public long getCurrentTransactionId() {
    return currentId;
  }

  @Override
  public long getOldestTransactionId() {
    return oldestid;
  }

  @Override
  public boolean isValidClientInformation() {
    return currentId >= 0 && sourceId.toLong() >= 0;
  }

  @Override
  public ClientSourceId makeClientSourceId(long l) {
    return null;
  }

  @Override
  public String toString() {
    return "InvokeContextImpl{" + "oldestid=" + oldestid + ", currentId=" + currentId + ", sourceId=" + sourceId + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    InvokeContextImpl context = (InvokeContextImpl) o;

    if (oldestid != context.oldestid) {
      return false;
    }
    if (currentId != context.currentId) {
      return false;
    }
    return sourceId != null ? sourceId.equals(context.sourceId) : context.sourceId == null;
  }

  @Override
  public int hashCode() {
    int result = (int) (oldestid ^ (oldestid >>> 32));
    result = 31 * result + (int) (currentId ^ (currentId >>> 32));
    result = 31 * result + (sourceId != null ? sourceId.hashCode() : 0);
    return result;
  }
}
