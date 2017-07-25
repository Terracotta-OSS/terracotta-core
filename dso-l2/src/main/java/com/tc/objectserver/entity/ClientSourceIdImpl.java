package com.tc.objectserver.entity;

import com.tc.net.ClientID;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ClientSourceId;

public class ClientSourceIdImpl implements ClientSourceId {
  public static final ClientSourceId NULL_ID = new ClientSourceIdImpl(ClientID.NULL_ID.toLong());

  final long id;

  public ClientSourceIdImpl(long id) {
    this.id = id;
  }

  public ClientSourceIdImpl() {
    this(NULL_ID.toLong());
  }

  @Override
  public long toLong() {
    return id;
  }

  @Override
  public boolean matches(ClientDescriptor descriptor) {
    return descriptor.getSourceId().toLong() == id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ClientSourceIdImpl id1 = (ClientSourceIdImpl) o;

    return id == id1.id;
  }

  @Override
  public int hashCode() {
    return (int) (id ^ (id >>> 32));
  }
}
