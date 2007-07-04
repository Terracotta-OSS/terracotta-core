/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.server.appserver.unit;


/**
 * Applies a simple filter to the {@link SimpleDsoSessionsTest}.
 */
public final class SimpleSessionFilterTest extends SimpleDsoSessionsTest {

  public SimpleSessionFilterTest() {
    this.addSessionFilter(SimpleFilter.class);
  }  
}
