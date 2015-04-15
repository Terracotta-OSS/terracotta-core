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
package com.tc.lang;

import com.tc.logging.TCLogger;

/**
* @author tim
*/
public class TestThrowableHandler extends ThrowableHandlerImpl {
  private volatile Throwable throwable;

  /**
   * Construct a new ThrowableHandler with a logger
   *
   * @param logger Logger
   */
  public TestThrowableHandler(final TCLogger logger) {
    super(logger);
  }

  @Override
  public void handleThrowable(final Thread thread, final Throwable t) {
    this.throwable = t;
    super.handleThrowable(thread, t);
  }

  public void throwIfNecessary() throws Throwable {
    if (throwable != null) { throw throwable;
    }
  }

  @Override
  protected synchronized void exit(final int status) {
    // don't do a system.exit.
  }
}
