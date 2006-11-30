/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.exception.TCException;

/**
 * @author Orion Letizi
 */
public class TCMessageException extends TCException {

  /**
   * 
   */
  public TCMessageException() {
    super();
  }

  /**
   * @param arg0
   */
  public TCMessageException(String arg0) {
    super(arg0);
  }

  /**
   * @param arg0
   */
  public TCMessageException(Throwable arg0) {
    super(arg0);
  }

  /**
   * @param arg0
   * @param arg1
   */
  public TCMessageException(String arg0, Throwable arg1) {
    super(arg0, arg1);
  }

}