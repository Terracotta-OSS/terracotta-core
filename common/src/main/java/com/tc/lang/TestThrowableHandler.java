/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.lang;

import org.slf4j.Logger;


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
  public TestThrowableHandler(Logger logger) {
    super(logger);
  }

  @Override
  public void handleThrowable(Thread thread, Throwable t) {
    this.throwable = t;
    super.handleThrowable(thread, t);
  }

  public void throwIfNecessary() throws Throwable {
    if (throwable != null) { throw throwable;
    }
  }

  @Override
  protected synchronized void exit(int status) {
    // don't do a system.exit.
  }
}
