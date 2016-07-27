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
 * The interface for methods which the clients can call which cause changes to the other processes running in the harness or
 * otherwise cause visibility between them.
 * Note that an implementation must expect that it will be called on different threads and is expected to act as a
 * heavy-weight interlock, forcing all calls into it to serialize so that there are never concerns of interactions between
 * calls from different clients.
 */
public interface IMultiProcessControl {
  public void synchronizeClient();

  public void terminateActive();

  public void startOneServer();

  public void terminateAllServers();

  public void waitForActive();

  public void waitForRunningPassivesInStandby();
}
