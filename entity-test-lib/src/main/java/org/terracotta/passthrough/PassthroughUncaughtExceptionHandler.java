/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.lang.Thread.UncaughtExceptionHandler;


/**
 * The uncaught exception handler installed for all the threads in the passthrough testing system.  All it does is log the
 * error and terminate the VM.
 */
public class PassthroughUncaughtExceptionHandler implements UncaughtExceptionHandler {
  public static final PassthroughUncaughtExceptionHandler sharedInstance = new PassthroughUncaughtExceptionHandler();

  @Override
  public void uncaughtException(Thread arg0, Throwable arg1) {
    System.err.println("FATAL EXCEPTION IN PASSTHROUGH THREAD:");
    arg1.printStackTrace();
    System.exit(1);
  }
}
