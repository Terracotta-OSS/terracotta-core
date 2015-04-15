/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
