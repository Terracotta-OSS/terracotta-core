/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.terracotta.management.security;

/**
 * @author brandony
 */
public class InvalidRequestTicketException extends Exception {
  public InvalidRequestTicketException() {
    super();
  }

  public InvalidRequestTicketException(String message) {
    super(message);
  }
}
