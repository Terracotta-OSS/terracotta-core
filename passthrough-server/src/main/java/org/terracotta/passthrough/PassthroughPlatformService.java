/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.passthrough;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.terracotta.monitoring.PlatformService;
import org.terracotta.monitoring.PlatformStopException;

/**
 * @author vmad
 */
public class PassthroughPlatformService implements PlatformService {

  private final PassthroughClusterControl passthroughClusterControl;
  private final PassthroughServer passthroughServer;

  public PassthroughPlatformService(PassthroughClusterControl passthroughClusterControl, PassthroughServer passthroughServer) {
    this.passthroughClusterControl = passthroughClusterControl;
    this.passthroughServer = passthroughServer;
  }

  @Override
  public void dumpPlatformState() {
    passthroughServer.dump();
  }

  @Override
  public void stopPlatform() {
    stopPlatform(RestartMode.STOP_ONLY);
  }

  @Override
  public InputStream getPlatformConfiguration() {
    return new ByteArrayInputStream(new byte[0]);
  }

  @Override
  public void stopPlatformIfPassive(RestartMode restartMode) throws PlatformStopException {
    passthroughClusterControl.terminateIfPassive(passthroughServer, restartMode);
  }

  @Override
  public void stopPlatformIfActive(RestartMode restartMode) throws PlatformStopException {
    passthroughClusterControl.terminateIfActive(passthroughServer, restartMode);
  }

  @Override
  public void stopPlatform(RestartMode restartMode) {
    passthroughClusterControl.terminate(passthroughServer, restartMode);
  }

  @Override
  public void fatalError(String string) {
    passthroughServer.stop();
  }

  @Override
  public long uptime() {
    return 0L;
  }
    
    
}
