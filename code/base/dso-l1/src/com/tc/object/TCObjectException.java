/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object;

/**
 * @author orion
 */
public class TCObjectException extends Exception {
  TCObjectException() {
    super();
  }

  TCObjectException(Exception e) {
    super(e);
  }

  TCObjectException(String message) {
    super(message);
  }
}