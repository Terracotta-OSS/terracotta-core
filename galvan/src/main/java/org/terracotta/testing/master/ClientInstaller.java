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

import java.io.File;

import org.terracotta.testing.logging.VerboseManager;


/**
 * Manages the installation of client processes.
 * This doesn't explicitly treat setup/destroy/test clients any differently, leaving that distinction up to the caller.
 */
public class ClientInstaller {
  private final VerboseManager clientsVerboseManager;
  private final IMultiProcessControl control;
  private final String testParentDirectory;
  private final String clientAbsoluteClassPath;
  private final String clientClassName;
  private final String testClassName;
  private final String stripeUri;
  
  public ClientInstaller(VerboseManager clientsVerboseManager, IMultiProcessControl control, String testParentDirectory, String clientClassPath, String clientClassName, String testClassName, String stripeUri) {
    this.clientsVerboseManager = clientsVerboseManager;
    this.control = control;
    this.testParentDirectory = testParentDirectory;
    // The client class path may have path separators in it so be sure to convert anything referenced there into an absolute
    // path, since we are going to change working directory when invoking the client processes.
    this.clientAbsoluteClassPath = makePathsAbsolute(clientClassPath);
    this.clientClassName = clientClassName;
    this.testClassName = testClassName;
    this.stripeUri = stripeUri;
  }
  
  public ClientRunner installClient(String clientName, String clientTask, int debugPort) {
    String clientWorkingDirectory = FileHelpers.createTempEmptyDirectory(this.testParentDirectory, clientName);
    VerboseManager clientVerboseManager = this.clientsVerboseManager.createComponentManager("[" + clientTask + "]");
    return new ClientRunner(clientVerboseManager, this.control, new File(clientWorkingDirectory), this.clientAbsoluteClassPath, this.clientClassName, clientTask, this.testClassName, this.stripeUri, debugPort);
  }
  
  
  /**
   * Just a helper used to make potentially relative paths into absolute paths (since the current directory changes when running clients).
   */
  private static String makePathsAbsolute(String parsedClientClassPath) {
    String[] relativePaths = parsedClientClassPath.split(File.pathSeparator);
    String concatenatedAbsolutePaths = "";
    boolean doesNeedSeparator = false;
    for (String oneRelativePath : relativePaths) {
      String oneAbsolutePath = new File(oneRelativePath).getAbsolutePath();
      if (doesNeedSeparator) {
        concatenatedAbsolutePaths += File.pathSeparator;
      }
      concatenatedAbsolutePaths += oneAbsolutePath;
      doesNeedSeparator = true;
    }
    return concatenatedAbsolutePaths;
  }
}
