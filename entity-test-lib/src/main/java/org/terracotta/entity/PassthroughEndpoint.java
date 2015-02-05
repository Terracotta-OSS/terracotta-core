package org.terracotta.entity;

import com.google.common.util.concurrent.Futures;

import java.io.Serializable;
import java.util.concurrent.Future;

/**
 * @author twu
 */
public class PassthroughEndpoint implements EntityClientEndpoint {
  private final ServerEntity entity;

  public PassthroughEndpoint(ServerEntity entity) {
    this.entity = entity;
  }

  @Override
  public void setEntityConfiguration(Serializable entityConfiguration) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public Serializable getEntityConfiguration() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void registerListener(EndpointListener listener) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public InvocationBuilder beginInvoke() {
    return new InvocationBuilderImpl();
  }

  private class InvocationBuilderImpl implements InvocationBuilder {
    private boolean returnsValue = false;
    private byte[] payload = null;

    @Override
    public InvocationBuilder returnsValue(boolean returnsValue) {
      this.returnsValue = returnsValue;
      return this;
    }

    @Override
    public InvocationBuilder payload(byte[] payload) {
      this.payload = payload;
      return this;
    }

    @Override
    public Future<?> invoke() {
      try {
        byte[] value = entity.invoke(payload);
        return returnsValue ? Futures.immediateFuture(value) : Futures.immediateFuture(null);
      } catch (Exception e) {
        return Futures.immediateFailedCheckedFuture(e);
      }
    }
  }
}
