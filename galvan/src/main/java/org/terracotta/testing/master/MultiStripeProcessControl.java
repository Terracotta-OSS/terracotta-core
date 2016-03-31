/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.master;


/**
 * This just wraps a list of IMultiProcessControl implementations, relaying all commands to all of them.
 * Additionally, since we still want the entire cluster to be controlled in lock-step, we will synchronize every method.
 */
public class MultiStripeProcessControl implements IMultiProcessControl {
  private final IMultiProcessControl[] subControl;

  public MultiStripeProcessControl(IMultiProcessControl[] subControl) {
    this.subControl = subControl;
  }

  @Override
  public synchronized void synchronizeClient() {
    for (IMultiProcessControl oneControl : this.subControl) {
      oneControl.synchronizeClient();
    }
  }

  @Override
  public synchronized void restartActive() {
    for (IMultiProcessControl oneControl : this.subControl) {
      oneControl.restartActive();
    }
  }

  @Override
  public synchronized void shutDown() {
    for (IMultiProcessControl oneControl : this.subControl) {
      oneControl.shutDown();
    }
  }

  @Override
  public synchronized void waitForActive() {
    for (IMultiProcessControl oneControl : this.subControl) {
      oneControl.waitForActive();
    }
  }

  @Override
  public synchronized void waitForPassive() {
    for (IMultiProcessControl oneControl : this.subControl) {
      oneControl.waitForPassive();
    }
  }
}
