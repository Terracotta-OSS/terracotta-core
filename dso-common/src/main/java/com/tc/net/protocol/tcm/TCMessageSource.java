/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.util.TCTimeoutException;

/**
 * Interface for classes that produce/provide TC Messages
 * 
 * @author teck
 */
public interface TCMessageSource {
  public TCMessage getMessage(long timeout) throws InterruptedException, TCTimeoutException;

  /**
   * Non-blocking getMessage() call
   */
  public TCMessage poll();
}
