/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.terracotta.testing.rules;

import java.io.File;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class BasicInlineClusterIT {
  
  private static Logger LOGGER = LoggerFactory.getLogger(BasicInlineClusterIT.class);
  
  public BasicInlineClusterIT() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

  /**
   */
  @Test
  public void testInlineServerIsGarbageCollected() throws Exception {
    ReferenceQueue queue = new ReferenceQueue();
    Reference ref = startThenStop(queue);
    while (queue.poll() != ref) {
      Thread.sleep(1000);
      System.gc();
      LOGGER.info("waiting for garbage collection of server");
    }
  }
  
  private static Reference startThenStop(ReferenceQueue queue) throws Exception {
    String kitInstallationPath = System.getProperty("kitInstallationPath");
    System.setProperty("tc.install-root", kitInstallationPath + File.separator + "server");
    Path tc = Paths.get(kitInstallationPath, "server", "lib", "tc.jar");
    URL url = tc.toUri().toURL();
    Object obj = BasicInlineCluster.startIsolatedServer(Paths.get("."), System.out, new String[] {});
    Future<Boolean> started = (Future)obj;
    Method m = started.getClass().getMethod("getServer");
    m.setAccessible(true);
    Reference ref = new PhantomReference(m.invoke(started), queue);
    ((Future)obj).cancel(true);
    ((Future)obj).get();
    return  ref;
  }

}
