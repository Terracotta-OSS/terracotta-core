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

import java.util.concurrent.Callable;

/**
 * A {@link ThrowableHandler} for Terracotta Client which avoids {@link System#exit(int)} on inconsistent state of
 * Terracotta Client. This handler will shutdown Terracotta Client instead through l1ShutdownCallable.
 */
public class L1ThrowableHandler extends ThrowableHandlerImpl {
  private final Callable<Void> l1ShutdownCallable;

  public L1ThrowableHandler(TCLogger logger, Callable<Void> l1ShutdownCallable) {
    super(logger);
    this.l1ShutdownCallable = l1ShutdownCallable;
  }

  @Override
  protected synchronized void exit(int status) {
    try {
      l1ShutdownCallable.call();
    } catch (Exception e) {
      logger.error("Exception while shutting down Terracotta Client", e);
    }
  }

}
