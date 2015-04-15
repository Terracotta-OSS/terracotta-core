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
package com.terracotta.toolkit;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.util.concurrent.ThreadUtil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NonStopClusterInfoTest {
  @Test
  public void testRemoveOnNodeErrorListener() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final AsyncToolkitInitializer asyncToolkitInitializer = mock(AsyncToolkitInitializer.class);
    when(asyncToolkitInitializer.getToolkit()).thenAnswer(new Answer<ToolkitInternal>() {
      @Override
      public ToolkitInternal answer(final InvocationOnMock invocationOnMock) throws Throwable {
        // use a latch to simulate the toolkit not being ready until after we register our listener
        latch.await();
        throw new RuntimeException("expected! we're pretending we can't connect");
      }
    });
    final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
    // Use a thread group for tracking when the test finishes and using a custom uncaught exception handler to save
    // any potential exception
    ThreadGroup threadGroup = new ThreadGroup("foo") {
      @Override
      public void uncaughtException(final Thread t, final Throwable e) {
        error.set(e);
      }
    };

    // startup the nonstopclusterinfo in the thread group to force it and the thread it spawns to sit in the
    // custom thread group (for tracking any uncaught exceptions)
    new Thread(threadGroup, new Runnable() {
      @Override
      public void run() {
        final NonStopClusterInfo nonStopClusterInfo = new NonStopClusterInfo(asyncToolkitInitializer);
        nonStopClusterInfo.addClusterListener(new ClusterListener() {
          @Override
          public void onClusterEvent(final ClusterEvent clusterEvent) {
            if (clusterEvent.getType() == ClusterEvent.Type.NODE_ERROR) {
              nonStopClusterInfo.removeClusterListener(this);
            }
          }
        });
        latch.countDown();
      }
    }).start();
    // Wait till all the threads are dead before checking for an error.
    while(threadGroup.activeCount() > 0) {
      ThreadUtil.reallySleep(1000);
    }
    assertNull(error.get());
  }
}