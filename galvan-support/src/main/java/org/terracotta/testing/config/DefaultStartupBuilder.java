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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import static org.terracotta.testing.demos.TestHelpers.isWindows;

public class DefaultStartupBuilder implements StartupBuilder {
  private Path tcConfig;
  private Path kitDir;
  private Path serverWorkingDirectory;
  private String logbackExtension;
  private String serverName;
  private boolean consistent;
  private String[] built;

  public DefaultStartupBuilder() {
  }

  private DefaultStartupBuilder(Path tcConfig, Path serverWorkingDirectory, String serverName) {
    this.tcConfig = tcConfig;
    this.serverWorkingDirectory = serverWorkingDirectory;
    this.serverName = serverName;
  }

  @Override
  public StartupBuilder tcConfig(Path tcConfig) {
    this.tcConfig = tcConfig;
    return this;
  }

  @Override
  public StartupBuilder serverWorkingDirectory(Path serverInstallationDir) {
    this.serverWorkingDirectory = serverInstallationDir;
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
  public StartupBuilder serverKitDir(Path kitDir) {
    this.kitDir = kitDir;
    return this;
  }

  @Override
  public StartupBuilder serverLoggingExtension(String logging) {
    this.logbackExtension = logging;
    return this;
  }

  protected void installServer() throws IOException {
    // Create a copy of the server for this installation.
    Files.createDirectories(this.serverWorkingDirectory);

    //Copy a custom logback configuration
    Files.copy(this.getClass().getResourceAsStream("/tc-logback.xml"), this.serverWorkingDirectory.resolve("logback-test.xml"), REPLACE_EXISTING);

    if (this.logbackExtension != null) {
      InputStream logExt = this.getClass().getResourceAsStream("/" + this.logbackExtension);
      if (logExt != null) {
        Files.copy(logExt, this.serverWorkingDirectory.resolve("logback-ext-test.xml"), REPLACE_EXISTING);
      }    
    }
  }
  
  @Override
  public String[] build() {
    if (built == null) {
      try {
        installServer();
        Path basePath = kitDir.resolve("server").resolve("bin").resolve("start-tc-server");
        String startScript = isWindows() ? basePath + ".bat" : basePath + ".sh";
        if (this.consistent) {
          built = new String[]{startScript, "-c", "-f", tcConfig.toString(), "-n", serverName};
        } else {
          built = new String[]{startScript, "-f", tcConfig.toString(), "-n", serverName};
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return built;
  }
}
