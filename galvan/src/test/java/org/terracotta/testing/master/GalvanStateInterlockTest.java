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

import org.junit.Before;
import org.junit.Test;
import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseLogger;

import static org.mockito.Mockito.mock;


/**
 * Unit tests for GalvanStateInterlock.
 * 
 * Note that this object is intended to be used in multiple threads and implements a basic inter-thread interlock so these
 *  tests also must be multi-threaded.
 */
public class GalvanStateInterlockTest {
  private GalvanStateInterlock interlock;
  private TestWaiter testWaiter;


  @Before
  public void setUp() {
    this.testWaiter = new TestWaiter();
    ContextualLogger logger = new ContextualLogger(new VerboseLogger(System.out, System.err), "test");
    this.interlock = new GalvanStateInterlock(logger, this.testWaiter);
  }

  @Test
  public void testSingleServerStart() throws Exception {
    Thread testThread = new Thread() {
      @Override
      public void run() {
        ServerProcess process = mock(ServerProcess.class);
        GalvanStateInterlockTest.this.interlock.registerNewServer(process);
        GalvanStateInterlockTest.this.interlock.serverDidStartup(process);
        GalvanStateInterlockTest.this.interlock.serverBecameActive(process);
      }
    };
    testThread.start();
    this.interlock.waitForAllServerRunning();
    this.interlock.waitForActiveServer();
    testThread.join();
  }

  @Test
  public void testZapUnknown() throws Exception {
    Thread testThread = new Thread() {
      @Override
      public void run() {
        ServerProcess active = mock(ServerProcess.class);
        GalvanStateInterlockTest.this.interlock.registerNewServer(active);
        ServerProcess passive = mock(ServerProcess.class);
        GalvanStateInterlockTest.this.interlock.registerNewServer(passive);
        // Have them both come online.
        GalvanStateInterlockTest.this.interlock.serverDidStartup(active);
        GalvanStateInterlockTest.this.interlock.serverDidStartup(passive);
        // Bring the active online.
        GalvanStateInterlockTest.this.interlock.serverBecameActive(active);
        // Zap the unknown server.
        GalvanStateInterlockTest.this.interlock.serverWasZapped(passive);
        // Start up the zapped server and have it come up as a passive.
        GalvanStateInterlockTest.this.interlock.serverDidStartup(passive);
        GalvanStateInterlockTest.this.interlock.serverBecamePassive(passive);
      }
    };
    testThread.start();
    this.interlock.waitForAllServerRunning();
    this.interlock.waitForAllServerReady();
    testThread.join();
  }

  @Test
  public void testZapPassive() throws Exception {
    Thread testThread = new Thread() {
      @Override
      public void run() {
        ServerProcess active = mock(ServerProcess.class);
        GalvanStateInterlockTest.this.interlock.registerNewServer(active);
        ServerProcess passive = mock(ServerProcess.class);
        GalvanStateInterlockTest.this.interlock.registerNewServer(passive);
        // (we also want to create a slow passive to extend the test until this is done).
        ServerProcess slow = mock(ServerProcess.class);
        GalvanStateInterlockTest.this.interlock.registerNewServer(slow);
        
        // Have them all come online.
        GalvanStateInterlockTest.this.interlock.serverDidStartup(active);
        GalvanStateInterlockTest.this.interlock.serverDidStartup(passive);
        GalvanStateInterlockTest.this.interlock.serverDidStartup(slow);
        // Bring the active online.
        GalvanStateInterlockTest.this.interlock.serverBecameActive(active);
        // Bring the passive online.
        GalvanStateInterlockTest.this.interlock.serverBecamePassive(passive);
        // Zap the passive.
        GalvanStateInterlockTest.this.interlock.serverWasZapped(passive);
        // Start up the zapped server and have it come up as a passive.
        GalvanStateInterlockTest.this.interlock.serverDidStartup(passive);
        GalvanStateInterlockTest.this.interlock.serverBecamePassive(passive);
        // Now, have the slow server become passive.
        GalvanStateInterlockTest.this.interlock.serverBecamePassive(slow);
      }
    };
    testThread.start();
    this.interlock.waitForAllServerRunning();
    this.interlock.waitForAllServerReady();
    testThread.join();
  }

  @Test
  public void testZapActive() throws Exception {
    Thread testThread = new Thread() {
      @Override
      public void run() {
        ServerProcess active = mock(ServerProcess.class);
        GalvanStateInterlockTest.this.interlock.registerNewServer(active);
        ServerProcess passive = mock(ServerProcess.class);
        GalvanStateInterlockTest.this.interlock.registerNewServer(passive);
        // (we also want to create a slow passive to extend the test until this is done).
        ServerProcess slow = mock(ServerProcess.class);
        GalvanStateInterlockTest.this.interlock.registerNewServer(slow);
        
        // Have them all come online.
        GalvanStateInterlockTest.this.interlock.serverDidStartup(active);
        GalvanStateInterlockTest.this.interlock.serverDidStartup(passive);
        GalvanStateInterlockTest.this.interlock.serverDidStartup(slow);
        // Bring the active online.
        GalvanStateInterlockTest.this.interlock.serverBecameActive(active);
        // Bring the passive online.
        GalvanStateInterlockTest.this.interlock.serverBecamePassive(passive);
        // Zap the active.
        GalvanStateInterlockTest.this.interlock.serverWasZapped(active);
        // Promote the passive.
        GalvanStateInterlockTest.this.interlock.serverBecameActive(passive);
        // Start up the zapped server and have it come up as a passive.
        GalvanStateInterlockTest.this.interlock.serverDidStartup(active);
        GalvanStateInterlockTest.this.interlock.serverBecamePassive(active);
        // Now, have the slow server become passive.
        GalvanStateInterlockTest.this.interlock.serverBecamePassive(slow);
      }
    };
    testThread.start();
    this.interlock.waitForAllServerRunning();
    this.interlock.waitForAllServerReady();
    testThread.join();
  }


  private static class TestWaiter implements ITestWaiter {
    @Override
    public boolean checkDidPass() throws GalvanFailureException {
      // We currently don't explicitly set the pass state so always return false.
      return false;
    }
  }
}
