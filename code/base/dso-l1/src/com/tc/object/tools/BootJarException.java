/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.tools;

public abstract class BootJarException extends Exception {
  
  protected BootJarException(String message){
    super(message);
  }
  
  protected BootJarException(String message, Throwable t){
    super(message, t);
  }
  
}
