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
package org.terracotta.testing.client;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URI;
import java.util.Properties;

import org.terracotta.connection.Connection;
import org.terracotta.passthrough.ICommonTest;
import org.terracotta.testing.common.Assert;


public class TestClientStub {
  /**
   * Arguments are always passed as pairs ("--identifier value") with the following identifiers (all mandatory):
   * --task: SETUP, TEST, or DESTROY
   * --testClass:  <full name of test class>
   * --connectUri:  <the cluster URI used in the test>
   * --totalClientCount:  <number of clients running the test>
   * --thisClientIndex:  <the 0-indexed number of this client instance>
   */
  public static void main(String[] args) throws Throwable {
    // Before anything, set the default exception handler.
    Thread.setDefaultUncaughtExceptionHandler(new ClientExceptionHandler());
    
    // Load all the required parameters:
    String task = readArgString(args, "--task");
    String testClassName = readArgString(args, "--testClass");
    String connectUri = readArgString(args, "--connectUri");
    int totalClientCount = readArgInt(args, "--totalClientCount");
    int thisClientIndex = readArgInt(args, "--thisClientIndex");
    
    boolean isSetup = task.equals("SETUP");
    boolean isTest = task.equals("TEST");
    boolean isDestroy = task.equals("DESTROY");
    Assert.assertTrue(isSetup || isTest || isDestroy);
    
    // We want to create the client-side IPC manager and use it to sync with the harness.
    ClientSideIPCManager manager = new ClientSideIPCManager(System.in, System.out);
    manager.sendSyncAndWait();
    
    System.out.println("Client task \"" + task + "\" in class: \"" + testClassName + "\"");
    Class<?> testClass = Thread.currentThread().getContextClassLoader().loadClass(testClassName);
    Object instance = testClass.getConstructors()[0].newInstance();
    Class<ICommonTest> interfaceClass = ICommonTest.class;
    ICommonTest test = interfaceClass.cast(instance);
    
    IPCClusterControl clusterControl = new IPCClusterControl(manager, new URI(connectUri), new Properties());
    Connection connection = clusterControl.createConnectionToActive();
    
    // Get the environment (we will pass this in all cases but it is only useful for TEST modes).
    ClientTestEnvironment env = new ClientTestEnvironment(connectUri, totalClientCount, thisClientIndex);
    
    if (isSetup) {
      test.runSetup(env, clusterControl, connection);
    }
    if (isTest) {
      test.runTest(env, clusterControl, connection);
    }
    if (isDestroy) {
      test.runDestroy(env, clusterControl, connection);
    }
    connection.close();
    manager.sendShutDownAndWait();
    
    // Note that this explicit exit seems to be required when attached to an active-active cluster.
    // TODO:  Determine why that is so we can remove this (it probably indicates that a non-daemon thread is still running).
    System.exit(0);
  }


  private static int readArgInt(String[] args, String identifier) {
    String stringValue = readArgString(args, identifier);
    // We don't expect to miss parameters.
    Assert.assertNotNull(stringValue);
    // If this fails to parse and throws, we still want to treat that as a fatal error.
    return Integer.parseInt(stringValue);
  }

  private static String readArgString(String[] args, String identifier) {
    String value = null;
    for (int i = 0; i < (args.length-1); ++i) {
      if (identifier.equals(args[i])) {
        value = args[i+1];
        break;
      }
    }
    // This isn't expected to fail since our arg list is well-defined.
    Assert.assertNotNull(value);
    return value;
  }


  private static class ClientExceptionHandler implements UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread t, Throwable e) {
      // Log the error.
      e.printStackTrace();
      // We will return non-zero (99 will do) to flag the error.
      System.exit(99);
    }
  }
}
