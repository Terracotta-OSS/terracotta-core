/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
