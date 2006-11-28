/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.aop;

public class ExpectedException extends Exception {

  private String m_cause;

  public ExpectedException(String cause) {
    m_cause = cause;
  }

  public String toString() {
    return m_cause;
  }
}
