/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.loaders.IsolationClassLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * A little DSO client class. One use of this program (ie. the reason I'm writing it) is to easily create a few DSO
 * requests of an L2 so that I can profile the server's basic operations in OptimizeIt
 */
public class StupidSimpleDSOClient {

  private final HashMap root        = new HashMap();
  private final String  putCountKey = "putCount";

  public void run() {
    for (int i = 0; i < 10; i++) {
      synchronized (root) {
        System.out.println("put returned: " + root.put("key", "value"));
        incrementPutCount();
        System.out.println("put count: " + root.get(putCountKey));
      }

      synchronized (root) {
        System.out.println("value = " + root.get("key"));
      }

      synchronized (root) {
        root.remove("key");
        System.out.println(root.size());
      }
    }
  }

  private final void incrementPutCount() {
    StupidSimpleDSOClientCounter counter = (StupidSimpleDSOClientCounter) root.get(putCountKey);
    if (counter == null) {
      counter = new StupidSimpleDSOClientCounter();
      root.put(putCountKey, counter);
    }
    counter.incrementCount();
  }

  public static void main(String[] args) throws Exception {
    StandardTVSConfigurationSetupManagerFactory factory;

    factory = new StandardTVSConfigurationSetupManagerFactory(args, false, new FatalIllegalConfigurationChangeHandler());
    L1TVSConfigurationSetupManager configManager = factory.createL1TVSConfigurationSetupManager();
    PreparedComponentsFromL2Connection components = new PreparedComponentsFromL2Connection(configManager);
    IsolationClassLoader classLoader = new IsolationClassLoader(new StandardDSOClientConfigHelper(configManager),
                                                                components);

    Class clientClass = classLoader.loadClass(StupidSimpleDSOClient.class.getName());
    final Object client = clientClass.newInstance();
    final Method run = clientClass.getDeclaredMethod("run", new Class[] {});

    Thread t = new Thread() {
      public void run() {
        try {
          run.invoke(client, new Object[] {});
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    t.setContextClassLoader(classLoader);
    t.start();

    t.join();

    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    System.out.println("Hit ENTER to exit the program");
    reader.readLine();
    System.exit(0);
  }
}
