/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
