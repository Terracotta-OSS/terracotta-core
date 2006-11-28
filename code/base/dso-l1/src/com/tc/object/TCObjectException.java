/*
 * Created on Jan 5, 2004
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