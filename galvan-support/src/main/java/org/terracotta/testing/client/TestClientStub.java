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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.terracotta.passthrough.IClientTestEnvironment;
import org.terracotta.passthrough.IClusterInfo;
import org.terracotta.passthrough.ICommonTest;
import org.terracotta.passthrough.IServerInfo;
import org.terracotta.passthrough.SimpleClientTestEnvironment;
import org.terracotta.testing.api.IClientErrorHandler;
import org.terracotta.testing.common.Assert;
import org.terracotta.testing.master.ClusterInfo;
import org.terracotta.testing.master.ServerInfo;


public class TestClientStub {
  private static IClientTestEnvironment testEnvironment;
  private static IClientErrorHandler errorHandler;


  /**
   * Arguments are always passed as pairs ("--identifier value") with the following identifiers (all mandatory):
   * --task: SETUP, TEST, or DESTROY
   * --testClass:  <full name of test class>
   * --connectUri:  <the cluster URI used in the test>
   * --totalClientCount:  <number of clients running the test>
   * --thisClientIndex:  <the 0-indexed number of this client instance>
   * [--errorClass]:  An optional argument, specifies the name of the failure handler class
   */
  public static void main(String[] args) throws Throwable {
    // Before anything, set the default exception handler.
    Thread.setDefaultUncaughtExceptionHandler(new ClientExceptionHandler());
    
    // Load all the required parameters:
    String task = readArgString(args, "--task");
    String testClassName = readArgString(args, "--testClass");
    String connectUri = readArgString(args, "--connectUri");
    String clusterInfo = readArgString(args, "--clusterInfo");
    int totalClientCount = readArgInt(args, "--totalClientCount");
    int thisClientIndex = readArgInt(args, "--thisClientIndex");
    String errorClassName = readArgStringOrNull(args, "--errorClass");
    
    // First thing, we want to see if we were given an error class, since it will handle any errors we encounter through
    //  the test.
    // Note that this is technically optional.
    if (null != errorClassName) {
      Class<?> testClass = Thread.currentThread().getContextClassLoader().loadClass(errorClassName);
      Object instance = testClass.getConstructors()[0].newInstance();
      Class<IClientErrorHandler> interfaceClass = IClientErrorHandler.class;
      TestClientStub.errorHandler = interfaceClass.cast(instance);
    }
    
    // Get the environment (we will pass this in all cases but it is only useful for TEST modes).
    TestClientStub.testEnvironment = new SimpleClientTestEnvironment(connectUri, totalClientCount, thisClientIndex, getClusterInfo(clusterInfo));
    
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
    
    IPCClusterControl clusterControl = new IPCClusterControl(manager);
    if (isSetup) {
      test.runSetup(TestClientStub.testEnvironment, clusterControl);
    }
    if (isTest) {
      test.runTest(TestClientStub.testEnvironment, clusterControl);
    }
    if (isDestroy) {
      test.runDestroy(TestClientStub.testEnvironment, clusterControl);
    }
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
    String value = readArgStringOrNull(args, identifier);
    // This isn't expected to fail since this arg is required.
    Assert.assertNotNull(value);
    return value;
  }

  private static String readArgStringOrNull(String[] args, String identifier) {
    String value = null;
    for (int i = 0; i < (args.length-1); ++i) {
      if (identifier.equals(args[i])) {
        value = args[i+1];
        break;
      }
    }
    return value;
  }

  private static IClusterInfo getClusterInfo(String encoded) {
    ClusterInfo clusterInfo = ClusterInfo.decode(encoded);
    return new IClusterInfo() {

      @Override
      public IServerInfo getServerInfo(String s) {
        ServerInfo serverInfo = clusterInfo.getServerInfo(s);
        return new IServerInfo() {
          @Override
          public String getName() {
            return serverInfo.getName();
          }

          @Override
          public int getServerPort() {
            return serverInfo.getServerPort();
          }

          @Override
          public int getGroupPort() {
            return serverInfo.getGroupPort();
          }
        };
      }

      @Override
      public Collection<IServerInfo> getServersInfo() {
        List<IServerInfo> serverInfos = new ArrayList<>();

        for(ServerInfo serverInfo : clusterInfo.getServersInfo()) {
          final ServerInfo thisServerInfo = serverInfo;
          serverInfos.add(new IServerInfo() {
            @Override
            public String getName() {
              return thisServerInfo.getName();
            }

            @Override
            public int getServerPort() {
              return thisServerInfo.getServerPort();
            }

            @Override
            public int getGroupPort() {
              return thisServerInfo.getGroupPort();
            }
          });
        }
        return serverInfos;
      }
    };
  }


  private static class ClientExceptionHandler implements UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread thread, Throwable e) {
      System.err.println("UNCAUGHT TEST CLIENT EXCEPTION!  TERMINATING CLIENT!");
      // Log the error.
      e.printStackTrace();
      // If we have an error handler, ask it what it wants to do.
      if (null != TestClientStub.errorHandler) {
        // Explicitly handle any failure, here, since we are in the handler.
        try {
          TestClientStub.errorHandler.handleError(TestClientStub.testEnvironment, e);
        } catch (Throwable t) {
          System.err.println("UNCAUGHT EXCEPTION IN HANDLER: " + t.getLocalizedMessage());
          t.printStackTrace();
        }
      } else {
        System.err.println("NOTE:  No client-side error handler installed in test");
      }
      // We will return non-zero (99 will do) to flag the error.
      System.exit(99);
    }
  }
}
