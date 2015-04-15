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
package com.tc.logging;

public class CallbackOnExitState {
  private volatile boolean restartNeeded = false;
  private final Throwable  throwable;

  public CallbackOnExitState(Throwable t) {
    this.throwable = t;
  }

  public Throwable getThrowable() {
    return throwable;
  }

  public void setRestartNeeded() {
    this.restartNeeded = true;
  }

  public boolean isRestartNeeded() {
    return this.restartNeeded;
  }

  @Override
  public String toString() {
    return "CallbackOnExitState[Throwable: " + throwable.getClass() + "; RestartNeeded: " + isRestartNeeded() + "]";
  }
}
