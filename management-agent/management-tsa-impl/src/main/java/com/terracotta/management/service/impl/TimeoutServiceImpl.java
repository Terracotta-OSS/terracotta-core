/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import com.terracotta.management.service.TimeoutService;

/**
 * @author Ludovic Orban
 */
public class TimeoutServiceImpl implements TimeoutService {

  private final ThreadLocal<Long> tlTimeout = new ThreadLocal<Long>();

  private final long defaultTimeout;

  public TimeoutServiceImpl(long defaultTimeout) {
    this.defaultTimeout = defaultTimeout;
  }

  @Override
  public void setCallTimeout(long readTimeout) {
    tlTimeout.set(readTimeout);
  }

  @Override
  public void clearCallTimeout() {
    tlTimeout.remove();
  }

  @Override
  public long getCallTimeout() {
    Long timeout = tlTimeout.get();
    return timeout == null ? defaultTimeout : timeout;
  }
}
