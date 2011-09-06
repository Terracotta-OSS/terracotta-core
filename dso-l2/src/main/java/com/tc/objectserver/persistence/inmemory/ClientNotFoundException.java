/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.inmemory;

public class ClientNotFoundException extends Exception {
  public ClientNotFoundException() {
    super();
  }
  
  public ClientNotFoundException(String message) {
    super(message);
  }
}
