/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

public class ClientNotFoundException extends Exception {
  public ClientNotFoundException() {
    super();
  }
  
  public ClientNotFoundException(String message) {
    super(message);
  }
}
