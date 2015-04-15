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

import com.tc.logging.CallbackOnExitHandler;

public class TCThreadGroup extends ThreadGroup {

  private static final String    CLASS_NAME = TCThreadGroup.class.getName();

  private final ThrowableHandler throwableHandler;

  public static boolean currentThreadInTCThreadGroup() {
    return Thread.currentThread().getThreadGroup().getClass().getName().equals(CLASS_NAME);
  }

  public TCThreadGroup(ThrowableHandler throwableHandler) {
    this(throwableHandler, "TC Thread Group");
  }

  public TCThreadGroup(ThrowableHandler throwableHandler, String name) {
    super(name);
    this.throwableHandler = throwableHandler;
  }

  @Override
  public void uncaughtException(Thread thread, Throwable throwable) {
    throwableHandler.handleThrowable(thread, throwable);
  }

  public void addCallbackOnExitDefaultHandler(CallbackOnExitHandler callbackOnExitHandler) {
    throwableHandler.addCallbackOnExitDefaultHandler(callbackOnExitHandler);
  }
  
  public void addCallbackOnExitExceptionHandler(Class c, CallbackOnExitHandler callbackOnExitHandler) {
    throwableHandler.addCallbackOnExitExceptionHandler(c, callbackOnExitHandler);
  }
}
