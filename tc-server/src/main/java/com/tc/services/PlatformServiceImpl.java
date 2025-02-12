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
package com.tc.services;

import com.tc.exception.TCNotRunningException;
import com.tc.lang.ServerExitStatus;
import com.tc.logging.TCLogging;
import com.tc.server.TCServer;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.EnumSet;

import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;
import org.terracotta.monitoring.PlatformService;
import org.terracotta.monitoring.PlatformStopException;
import org.terracotta.server.ServerEnv;
import org.terracotta.server.StopAction;
import static org.terracotta.server.StopAction.RESTART;
import static org.terracotta.server.StopAction.ZAP;

/**
 * @author vmad
 */
public class PlatformServiceImpl implements PlatformService, StateDumpable {

    private final TCServer tcServer;

    public PlatformServiceImpl(TCServer tcServer) {
        this.tcServer = tcServer;
    }

    @Override
    public void dumpPlatformState() {
        tcServer.dump();
    }

    @Override
    public void stopPlatform() {
        tcServer.stop();
    }

    @Override
    public long uptime() {
      return System.currentTimeMillis() - tcServer.getStartTime();
    }

    @Override
    public void fatalError(String description) {
      TCLogging.getConsoleLogger().error("A fatal error occurred: " + description);
      System.exit(ServerExitStatus.EXITCODE_FATAL_ERROR);
    }

    @Override
    public InputStream getPlatformConfiguration() {
      return new ByteArrayInputStream(ServerEnv.getServer().getConfiguration().getBytes(Charset.defaultCharset()));
    }

    private StopAction[] convert(RestartMode mode) {
      switch (mode) {
        case STOP_AND_RESTART:
          return EnumSet.of(RESTART).toArray(new StopAction[0]);
        case STOP_ONLY:
          return EnumSet.noneOf(StopAction.class).toArray(new StopAction[0]);
        case ZAP_AND_RESTART:
          return EnumSet.of(ZAP,RESTART).toArray(new StopAction[0]);
        case ZAP_AND_STOP:
          return EnumSet.of(ZAP).toArray(new StopAction[0]);
        default:
          return EnumSet.noneOf(StopAction.class).toArray(new StopAction[0]);
      }
    }

    @Override
    public void stopPlatformIfPassive(RestartMode restartMode) throws PlatformStopException {
      tcServer.stopIfPassive(convert(restartMode));
      throw new TCNotRunningException("shutdown as PASSIVE");
    }

    @Override
    public void stopPlatformIfActive(RestartMode restartMode) throws PlatformStopException {
      tcServer.stopIfActive(convert(restartMode));
      throw new TCNotRunningException("shutdown as ACTIVE");
    }

    @Override
    public void stopPlatform(RestartMode restartMode) {
      tcServer.stop(convert(restartMode));
      throw new TCNotRunningException("shutdown");
    }

  @Override
  public void addStateTo(StateDumpCollector stateDumpCollector) {
    stateDumpCollector.addState("tcServerState", tcServer.getState());
    stateDumpCollector.addState("tcServerConfig", tcServer.getConfig());
  }
}
