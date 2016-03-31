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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.passthrough.ICommonTest;
import org.terracotta.testing.common.Assert;


public class TestClientStub {
  /**
   * We expect 3 args:
   * [0] - "SETUP", "TEST", or "DESTROY"
   * [1] - full name of test class
   * [2] - connect URI
   */
  public static void main(String[] args) throws InterruptedException, ConnectionException, URISyntaxException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, SecurityException {
    // Load the class.
    String task = args[0];
    boolean isSetup = task.equals("SETUP");
    boolean isTest = task.equals("TEST");
    boolean isDestroy = task.equals("DESTROY");
    Assert.assertTrue(isSetup || isTest || isDestroy);
    
    // We want to create the client-side IPC manager and use it to sync with the harness.
    ClientSideIPCManager manager = new ClientSideIPCManager(System.in, System.out);
    manager.sendSyncAndWait();
    
    String testClassName = args[1];
    System.out.println("Client task \"" + task + "\" in class: \"" + testClassName + "\"");
    Class<?> testClass = Thread.currentThread().getContextClassLoader().loadClass(testClassName);
    Object instance = testClass.getConstructors()[0].newInstance();
    Class<ICommonTest> interfaceClass = ICommonTest.class;
    ICommonTest test = interfaceClass.cast(instance);
    
    IPCClusterControl clusterControl = new IPCClusterControl(manager, new URI(args[2]), new Properties());
    Connection connection = clusterControl.createConnectionToActive();
    
    if (isSetup) {
      test.runSetup(connection);
    }
    if (isTest) {
      try {
        test.runTest(clusterControl, connection);
      } catch (Throwable e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    if (isDestroy) {
      test.runDestroy(connection);
    }
    connection.close();
    manager.sendShutDownAndWait();
    
    // Note that this explicit exit seems to be required when attached to an active-active cluster.
    // TODO:  Determine why that is so we can remove this (it probably indicates that a non-daemon thread is still running).
    System.exit(0);
  }
}
