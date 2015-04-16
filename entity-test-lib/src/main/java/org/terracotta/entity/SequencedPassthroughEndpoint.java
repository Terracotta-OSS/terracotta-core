package org.terracotta.entity;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by sairammv on 18/03/15.
 */
public class SequencedPassthroughEndpoint extends PassthroughEndpoint {
  private final Lock lock;
  public SequencedPassthroughEndpoint(ServerEntity entity) {
    super(entity);
    this.lock = new ReentrantLock();
  }

  @Override
  public InvocationBuilder beginInvoke() {
    lock.lock();
    try {
      return super.beginInvoke();
    } finally {
      lock.unlock();
    }
  }
}
