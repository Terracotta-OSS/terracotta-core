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
package com.tc.net.protocol.transport;

import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;

public class NullHealthCheckerSocketConnectImpl implements HealthCheckerSocketConnect {

  @Override
  public boolean probeConnectStatus() {
    return false;
  }

  @Override
  public SocketConnectStartStatus start() {
    return SocketConnectStartStatus.NOT_STARTED;
  }

  @Override
  public void closeEvent(TCConnectionEvent event) {
    //
  }

  @Override
  public void connectEvent(TCConnectionEvent event) {
    //
  }

  @Override
  public void endOfFileEvent(TCConnectionEvent event) {
    //
  }

  @Override
  public void errorEvent(TCConnectionErrorEvent errorEvent) {
    //
  }

  @Override
  public void addSocketConnectEventListener(HealthCheckerSocketConnectEventListener listener) {
    //
  }

  @Override
  public void removeSocketConnectEventListener(HealthCheckerSocketConnectEventListener listener) {
    //
  }

  @Override
  public void stop() {
    //
  }

}
