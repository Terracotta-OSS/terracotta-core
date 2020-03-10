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
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.testing.config;

import java.nio.file.Path;
import java.util.function.Supplier;

import static org.terracotta.testing.demos.TestHelpers.isWindows;

public class DefaultStartupBuilder implements StartupBuilder {
  private Path tcConfig;
  private Path serverInstallationDir;
  private String serverName;
  private boolean consistent;
  private boolean built;

  public DefaultStartupBuilder() {
  }

  private DefaultStartupBuilder(Path tcConfig, Path serverInstallationDir, String serverName) {
    this.tcConfig = tcConfig;
    this.serverInstallationDir = serverInstallationDir;
    this.serverName = serverName;
  }

  @Override
  public StartupBuilder tcConfig(Path tcConfig) {
    this.tcConfig = tcConfig;
    return this;
  }

  @Override
  public StartupBuilder serverInstallationDir(Path serverInstallationDir) {
    this.serverInstallationDir = serverInstallationDir;
    return this;
  }

  @Override
  public StartupBuilder testParentDir(Path testParentDir) {
    return this;
  }

  @Override
  public StartupBuilder serverName(String serverName) {
    this.serverName = serverName;
    return this;
  }

  @Override
  public StartupBuilder stripeName(String stripeName) {
    return this;
  }

  @Override
  public StartupBuilder consistentStartup(boolean consistent) {
    this.consistent = consistent;
    return this;
  }
  
  @Override
  public String[] build() {
    if (!built) {
      Path basePath = serverInstallationDir.resolve("server").resolve("bin").resolve("start-tc-server");
      String startScript = isWindows() ? basePath + ".bat" : basePath + ".sh";
      built = true;
      if (this.consistent) {
        return new String[]{startScript, "-c", "-f", tcConfig.toString(), "-n", serverName};
      } else {
        return new String[]{startScript, "-f", tcConfig.toString(), "-n", serverName};
      }
    } else {
      throw new AssertionError("startup already built");
    }
  }
}
