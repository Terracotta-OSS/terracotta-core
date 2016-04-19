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

import java.lang.reflect.InvocationTargetException;

import org.terracotta.testing.api.ITestClusterConfiguration;
import org.terracotta.testing.api.ITestMaster;


/**
 * Common utilities for command-line launchers (since they typically differ only in how they load their ITestMaster and
 * which AbstractHarnessEntry implementation they use).
 * We otherwise want all the launchers to parse the same config data from the command-line so that we can use them in the
 * common parts of the harness.
 */
public class CommandLineSupport {
  public static EnvironmentOptions parseEnvironmentOptions(String[] args) {
    EnvironmentOptions environmentOptions = new EnvironmentOptions();
    environmentOptions.clientClassPath = getArg(args, "--clientClassPath");
    environmentOptions.serverInstallDirectory = getArg(args, "--serverInstallDirectory");
    environmentOptions.testParentDirectory = getArg(args, "--testParentDirectory");
    return environmentOptions;
  }

  public static String parseTestMasterClass(String[] args) {
    return getArg(args, "--masterClassName");
  }

  public static DebugOptions parseDebugOptions(String[] args) {
    DebugOptions debugOptions = new DebugOptions();
    debugOptions.setupClientDebugPort = getIntArg(args, "--debugClientSetup");
    debugOptions.destroyClientDebugPort = getIntArg(args, "--debugClientDestroy");
    debugOptions.testClientDebugPortStart = getIntArg(args, "--debugClientsStart");
    return debugOptions;
  }

  public static boolean parseVerbose(String[] args) {
    return isArgSet(args, "--verbose");
  }

  public static String getUsageString() {
    return "Usage: TestMaster"
        + " --clientClassPath <client class path>"
        + " --serverInstallDirectory <server install directory>"
        + " --testParentDirectory <directory to empty and use for temporary installations>"
        + " --masterClassName <name of ITestMaster implementation>"
        + " [--debugClientSetup <port>]"
        + " [--debugClientDestroy <port>]"
        + " [--debugClientsStart <starting port>]"
        + " [--verbose]";
  }

  public static String getArg(String[] args, String argName) {
    String match = null;
    for (int i = 0; (null == match) && (i < (args.length - 1)); ++i) {
      if (argName.equals(args[i])) {
        match = args[i+1];
      }
    }
    return match;
  }

  public static int getIntArg(String[] args, String argName) {
    int result = 0;
    String match = getArg(args, argName);
    if (null != match) {
      result = Integer.parseInt(match);
    }
    return result;
  }

  public static boolean isArgSet(String[] args, String argName) {
    boolean didMatch = false;
    for (int i = 0; (!didMatch) && (i < args.length); ++i) {
      if (argName.equals(args[i])) {
        didMatch = true;
      }
    }
    return didMatch;
  }

  @SuppressWarnings("unchecked")
  public static <C extends ITestClusterConfiguration> ITestMaster<C> loadMaster(String parsedMasterClass) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, SecurityException {
    // Use the default class loader for this thread.
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    // First we load the class.
    Class<?> clazz = loader.loadClass(parsedMasterClass);
    // Create an instance - we assume that there is a no-arg constructor.
    Object instance = clazz.getConstructors()[0].newInstance();
    // Now, verify that it implements the ITestMaster interface by trying to cast it.
    @SuppressWarnings("rawtypes")
    Class<ITestMaster> interfaceClass = ITestMaster.class;
    return interfaceClass.cast(instance);
  }
}
