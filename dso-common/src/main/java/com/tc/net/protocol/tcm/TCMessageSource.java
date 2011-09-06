/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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