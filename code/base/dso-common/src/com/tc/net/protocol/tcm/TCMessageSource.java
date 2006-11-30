/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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