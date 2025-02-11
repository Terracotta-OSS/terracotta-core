/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.terracotta.testing.logging.VerboseManager;


/**
 * Manages the installation of client processes.
 * This doesn't explicitly treat setup/destroy/test clients any differently, leaving that distinction up to the caller.
 */
public class ClientInstaller {
  private final VerboseManager clientsVerboseManager;
  private final IMultiProcessControl control;
  private final Path testParentDirectory;
  private final String clientAbsoluteClassPath;
  private final String clientMainClassName;
  
  public ClientInstaller(VerboseManager clientsVerboseManager, IMultiProcessControl control, Path testParentDirectory, String clientClassPath, String clientMainClassName) {
    this.clientsVerboseManager = clientsVerboseManager;
    this.control = control;
    this.testParentDirectory = testParentDirectory;
    // The client class path may have path separators in it so be sure to convert anything referenced there into an absolute
    // path, since we are going to change working directory when invoking the client processes.
    this.clientAbsoluteClassPath = makePathsAbsolute(clientClassPath);
    this.clientMainClassName = clientMainClassName;
  }
  
  public ClientRunner installClient(String clientName, int debugPort, boolean failOnLog, List<String> extraArguments) {
    VerboseManager clientVerboseManager = this.clientsVerboseManager.createComponentManager("[" + clientName + "]");
    Path clientWorkingDirectory = FileHelpers.createTempEmptyDirectory(this.testParentDirectory, clientName);
    return new ClientRunner(clientVerboseManager, this.control, clientWorkingDirectory, this.clientAbsoluteClassPath, debugPort, failOnLog, this.clientMainClassName, extraArguments);
  }
  
  
  /**
   * Just a helper used to make potentially relative paths into absolute paths (since the current directory changes when running clients).
   */
  private static String makePathsAbsolute(String parsedClientClassPath) {
    String[] relativePaths = parsedClientClassPath.split(File.pathSeparator);
    StringBuilder concatenatedAbsolutePaths = new StringBuilder();
    boolean doesNeedSeparator = false;
    for (String oneRelativePath : relativePaths) {
      String oneAbsolutePath = new File(oneRelativePath).getAbsolutePath();
      if (doesNeedSeparator) {
        concatenatedAbsolutePaths.append(File.pathSeparator);
      }
      concatenatedAbsolutePaths.append(oneAbsolutePath);
      doesNeedSeparator = true;
    }
    return concatenatedAbsolutePaths.toString();
  }
}
