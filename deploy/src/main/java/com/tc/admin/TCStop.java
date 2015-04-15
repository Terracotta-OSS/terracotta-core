/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.admin;

import org.apache.commons.cli.Options;

import com.tc.cli.CommandLineBuilder;
import com.tc.cli.ManagementToolUtil;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.ConnectException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class TCStop {
  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

  private static final int MAX_TRIES = 50;
  private static final int TRY_INTERVAL = 1000;

  public static void main(String[] args) throws Exception {
    CommandLineBuilder commandLineBuilder = getCommandLineBuilder(args);

    if (commandLineBuilder.hasOption('h')) {
      commandLineBuilder.usageAndDie();
    }

    try {
      stop(args);
    } catch (SecurityException se) {
      consoleLogger.error(se.getMessage(), se);
      commandLineBuilder.usageAndDie();
    } catch (Exception e) {
      System.exit(1);
    }
  }

  public static void stop(final String[] args) throws Exception {
    CommandLineBuilder commandLineBuilder = getCommandLineBuilder(args);

    for (WebTarget target : ManagementToolUtil.getTargets(commandLineBuilder)) {
        try {
          restStop(target, commandLineBuilder.hasOption("force"));
        } catch (SecurityException se) {
          consoleLogger.error(se.getMessage(), se);
          throw se;
        } catch (Exception e) {
          Throwable root = getRootCause(e);
          if (root instanceof ConnectException) {
            consoleLogger.error("Unable to connect to host '" + target.getUri().getHost() + "', port " + target.getUri().getPort()
                                + ". Are you sure there is a Terracotta server instance running there?");
          } else {
            consoleLogger.error("Unexpected error while stopping server", root);
          }
          throw e;
        }
      }
  }

  protected static CommandLineBuilder getCommandLineBuilder(final String[] args) {Options options = StandardConfigurationSetupManagerFactory
    .createOptions(StandardConfigurationSetupManagerFactory.ConfigMode.L2);
    CommandLineBuilder commandLineBuilder = new CommandLineBuilder(TCStop.class.getName(), args);
    commandLineBuilder.setOptions(options);
    ManagementToolUtil.addConnectionOptionsTo(commandLineBuilder);
    commandLineBuilder.addOption("force", "force", false, "force", String.class, false);
    commandLineBuilder.addOption("h", "help", String.class, false);
    commandLineBuilder.parse();
    return commandLineBuilder;
  }

  private static Throwable getRootCause(Throwable e) {
    Throwable t = e;
    while (t != null) {
      e = t;
      t = t.getCause();
    }
    return e;
  }

  public static void restStop(WebTarget target, boolean force) throws IOException {
    for (int i = 0; i < MAX_TRIES; i++) {
      Entity<Map<String, Boolean>> forceStop = Entity.json(Collections.singletonMap("forceStop", force));
      Response response = target.path("/tc-management-api/v2/local/shutdown").request(MediaType.APPLICATION_JSON_TYPE)
          .post(forceStop);

      if (response.getStatus() >= 200 && response.getStatus() < 300) {
        consoleLogger.info("Stop success. Response code " + response.getStatus());
        return;
      } else if (response.getStatus() == 401) {
        consoleLogger.error("Authentication failure. Invalid username/password.");
        throw new IOException("Incorrect username/password");
      } else if (response.getStatus() == 404) {
        consoleLogger.info("Got a 404, waiting a bit before retrying.");
        ThreadUtil.reallySleep(TRY_INTERVAL);
      } else {
        Map<String, ?> errorResponse = response.readEntity(Map.class);
        consoleLogger.error(errorResponse.get("stackTrace"));
        throw new IOException("Error stopping server: " + errorResponse.get("error"));
      }
    }
    throw new IOException("Ran out of tries.");
  }

  public static void restStop(String host, int port, String username, String password, boolean force, boolean secured,
                              final boolean ignoreUntrusted)
      throws IOException, KeyManagementException, NoSuchAlgorithmException {
    restStop(ManagementToolUtil.targetFor(host, port, username, password, secured, ignoreUntrusted), force);
  }
}
