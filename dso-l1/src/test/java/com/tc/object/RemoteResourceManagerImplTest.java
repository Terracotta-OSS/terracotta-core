package com.tc.object;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.net.GroupID;
import com.tc.object.tx.RemoteTransactionManager;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author tim
 */
public class RemoteResourceManagerImplTest {
  private RemoteResourceManager remoteResourceManager;
  private GroupID groupID;
  private ObjectID objectID;

  @Before
  public void setUp() throws Exception {
    remoteResourceManager = new RemoteResourceManagerImpl(Mockito.mock(RemoteTransactionManager.class),Mockito.mock(AbortableOperationManager.class));
    groupID = new GroupID(1);
    objectID = new ObjectID(1);
  }

  @Test
  public void testThrowException() throws Exception {
    remoteResourceManager.handleThrottleMessage(groupID, false, 0.0f);
    // Shouldn't throw yet
    remoteResourceManager.throttleIfMutationIfNecessary(objectID);
    try {
      remoteResourceManager.handleThrottleMessage(groupID, true, 0.0f);
    } catch (OutOfResourceException e) {
      // expected
    }
  }

  @Test
  public void testThrottle() throws Exception {
    remoteResourceManager.handleThrottleMessage(groupID, false, 1.0f);
    long start = System.nanoTime();
    remoteResourceManager.throttleIfMutationIfNecessary(objectID);
    assertTrue(System.nanoTime() - start > TimeUnit.MILLISECONDS.toNanos(1500));
    remoteResourceManager.handleThrottleMessage(groupID, false, 0.0f);
    start = System.nanoTime();
    remoteResourceManager.throttleIfMutationIfNecessary(objectID);
    assertTrue(System.nanoTime() - start < TimeUnit.MILLISECONDS.toNanos(500));
  }

  @Test
  public void testUninitialized() throws Exception {
    final AtomicBoolean finished = new AtomicBoolean(false);
    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          remoteResourceManager.throttleIfMutationIfNecessary(objectID);
        } catch (AbortedOperationException e) {
          // should not happen
          e.printStackTrace();
        }
        finished.set(true);
      }
    });
    t.start();

    // Give it a second to reach the wait
    Thread.sleep(2000);

    assertFalse(finished.get());

    remoteResourceManager.handleThrottleMessage(groupID, false, 0.0f);

    t.join(2000);

    assertTrue(finished.get());
  }
}
