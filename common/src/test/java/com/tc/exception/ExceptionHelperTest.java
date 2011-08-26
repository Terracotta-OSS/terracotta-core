/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.exception;

import junit.framework.TestCase;

public class ExceptionHelperTest extends TestCase {
  public void test() {
    ExceptionHelperImpl helper = new ExceptionHelperImpl();
    helper.addHelper(new RuntimeExceptionHelper());

    Throwable ultimateCause = new AssertionError();
    Exception proximateCause = new RuntimeException(ultimateCause);
    Exception top = new TCRuntimeException(proximateCause);

    check(helper, ultimateCause, proximateCause, top);
  }

  private void check(ExceptionHelper helper, Throwable ultimateCause, Exception proximateCause, Exception top) {
    assertSame(ultimateCause, helper.getUltimateCause(top));
    assertSame(ultimateCause, helper.getUltimateCause(proximateCause));
    assertSame(ultimateCause, helper.getUltimateCause(ultimateCause));
    assertSame(proximateCause, helper.getProximateCause(top));
    assertSame(ultimateCause, helper.getProximateCause(proximateCause));
    assertSame(ultimateCause, helper.getProximateCause(ultimateCause));
  }
}
