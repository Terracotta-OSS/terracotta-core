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
    this.interlock.waitForAllServerReady();
    this.interlock.waitForActiveServer();
    testThread.join();
  }


  private static class TestWaiter implements ITestWaiter {
    @Override
    public void waitForFinish() throws GalvanFailureException {
      // Do nothing.
    }
    @Override
    public boolean checkDidPass() throws GalvanFailureException {
      // We currently don't explicitly set the pass state so always return false.
      return false;
    }
  }
}
