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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseLogger;


/**
 * This class is meant to be a container of helpers and testing idioms which are common to different entry-points into the framework.
 * It exists purely to avoid duplication.
 */
public class CommonIdioms {
  public static ReadyStripe setupConfigureAndStartStripe(ITestStateManager stateManager, VerboseLogger logger, ContextualLogger fileHelperLogger, String serverInstallDirectory, String testParentDirectory, int serversToCreate, int serverStartPort, int serverDebugPortStart, int serverStartNumber, boolean isRestartable, List<String> extraJarPaths, String namespaceFragment, String serviceFragment, String stripeName) throws IOException, FileNotFoundException {
    ContextualLogger stripeLogger = new ContextualLogger(logger, "[" + stripeName + "]");
    // We want to create a sub-directory per-stripe.
    String stripeParentDirectory = FileHelpers.createTempEmptyDirectory(testParentDirectory, stripeName);
    return ReadyStripe.configureAndStartStripe(stateManager, stripeLogger, fileHelperLogger, serverInstallDirectory, stripeParentDirectory, serversToCreate, serverStartPort, serverDebugPortStart, serverStartNumber, isRestartable, extraJarPaths, namespaceFragment, serviceFragment);
  }

  /**
   * Note that the clients will be run in another thread, logging to the given logger and returning their state in stateManager.
   */
  public static void installAndRunClients(ITestStateManager stateManager, VerboseLogger logger, String testParentDirectory, String clientClassPath, DebugOptions debugOptions, int clientsToCreate, String testClassName, IMultiProcessControl processControl, String connectUri) throws InterruptedException, IOException, FileNotFoundException {
    InterruptableClientManager manager = new InterruptableClientManager(stateManager, logger, testParentDirectory, clientClassPath, debugOptions, clientsToCreate, testClassName, processControl, connectUri);
    stateManager.addComponentToShutDown(manager);
    manager.start();
  }

  public static <T> List<T> uniquifyList(List<T> list) {
    Vector<T> newList = new Vector<>();
    for (T element : list) {
      if (!newList.contains(element)) {
        newList.add(element);
      }
    }
    return newList;
  }
}
