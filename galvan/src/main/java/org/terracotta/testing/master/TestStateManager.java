package org.terracotta.testing.master;

import java.util.List;
import java.util.Vector;

import org.terracotta.testing.common.Assert;


/**
 * The states of the clients and servers are run in different threads and each one of those could fail for various reasons
 * during a test run.
 * A given test will have its respective servers and clients created before creating this object but it acts as the waiting
 * point for the harness main thread until either a client or server fails or until the thread running the clients notifies
 * it that the test has completed.
 * Note that this will only be notified of those state changes which mark the test as either a pass or a fail, not any
 * expected activities, within a test.  For example, it will not be notified that a server terminated due to a client asking
 * it to restart, via the expected paths.  Therefore, there is no need to worry about those states, here.
 */
public class TestStateManager implements ITestStateManager {
  private final List<IComponentManager> componentsToShutDown = new Vector<IComponentManager>();
  private boolean didTestPass;
  private boolean didTestFail;

  /**
   * Waits until the test completes, as either a pass or a fail, blocking the calling thread.
   * 
   * @return True if the test passed, false if it failed.
   */
  public synchronized boolean waitForFinish() {
    // We expect that at least some asynchronous components have been registered.  While not required, missing them would
    // imply that there is a bug or inconsistent change within the current implementation so it seems worth checking.
    Assert.assertFalse(this.componentsToShutDown.isEmpty());
    
    while (!this.didTestPass && !this.didTestFail) {
      try {
        this.wait();
      } catch (InterruptedException e) {
        // We don't expect this thread to be interrupted, as it is the top-most point in the test.
        Assert.unexpected(e);
      }
    }
    
    // Shut down all components.
    for (IComponentManager component : this.componentsToShutDown) {
      component.forceTerminateComponent();
    }
    // If we set both pass and fail, this is still a fail.
    return !this.didTestFail;
  }

  @Override
  public synchronized void testDidPass() {
    this.didTestPass = true;
    notifyAll();
  }

  @Override
  public synchronized void testDidFail() {
    this.didTestFail = true;
    notifyAll();
  }

  @Override
  public synchronized void addComponentToShutDown(IComponentManager componentManager) {
    this.componentsToShutDown.add(componentManager);
  }
}
