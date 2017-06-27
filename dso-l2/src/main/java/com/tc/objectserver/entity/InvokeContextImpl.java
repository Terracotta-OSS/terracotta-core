package com.tc.objectserver.entity;

import com.tc.object.tx.TransactionID;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.InvokeContext;

public class InvokeContextImpl implements InvokeContext {

  public static InvokeContext NULL_CONTEXT=new InvokeContextImpl();

  private final ClientDescriptorImpl clientDescriptor;
  private final long oldestid;
  private final long currentId;

  private InvokeContextImpl() {
    this(ClientDescriptorImpl.NULL_ID, TransactionID.NULL_ID.toLong(), TransactionID.NULL_ID.toLong());
  }

  public InvokeContextImpl(ClientDescriptorImpl descriptor) {
    this(descriptor, TransactionID.NULL_ID.toLong(), TransactionID.NULL_ID.toLong());
  }

  public InvokeContextImpl(ClientDescriptorImpl descriptor, long oldestid, long currentId) {
    clientDescriptor = descriptor;
    this.oldestid = oldestid;
    this.currentId = currentId;
  }

  @Override
  public ClientDescriptor getClientDescriptor() {
    return clientDescriptor;
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
    return currentId >= 0 && clientDescriptor.isValid();
  }

  @Override
  public String toString() {
    return "InvokeContextImpl{" + "clientDescriptor=" + clientDescriptor + ", oldestid=" + oldestid + ", currentId=" + currentId + '}';
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
    return clientDescriptor != null ? clientDescriptor.equals(context.clientDescriptor) : context.clientDescriptor == null;
  }

  @Override
  public int hashCode() {
    int result = clientDescriptor != null ? clientDescriptor.hashCode() : 0;
    result = 31 * result + (int) (oldestid ^ (oldestid >>> 32));
    result = 31 * result + (int) (currentId ^ (currentId >>> 32));
    return result;
  }
}
