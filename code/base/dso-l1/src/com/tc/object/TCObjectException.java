/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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