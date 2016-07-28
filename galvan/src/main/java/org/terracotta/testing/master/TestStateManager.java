package org.terracotta.testing.master;

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
 * 
 * NOTE:  This is typically used as the single wait/notify monitor in galvan, as it represents the highest-level
 * synchronization object (it is passed in from the outside), so other components of the synchronization mechanism often
 * use it to coordinate synchronization across the framework.
 */
public class TestStateManager implements ITestWaiter, ITestStateManager {
  // ----- TEST STATE -----
  // The test either either running, did pass, or did set a failure exception (which is typically just a description
  //  of where the failure was observed).
  private boolean testDidPass;
  private GalvanFailureException testFailureException;


  @Override
  public synchronized void waitForFinish() throws GalvanFailureException {
    while (!this.testDidPass && (null == this.testFailureException)) {
      try {
        this.wait();
      } catch (InterruptedException e) {
        // We aren't expecting this, in these tests (as anyone could set our state to failed in order to force a failure).
        Assert.unexpected(e);
      }
    }
    if (null != this.testFailureException) {
      throw this.testFailureException;
    }
    Assert.assertTrue(this.testDidPass);
  }

  @Override
  public synchronized boolean checkDidPass() throws GalvanFailureException {
    if (null != this.testFailureException) {
      throw this.testFailureException;
    }
    return this.testDidPass;
  }

  @Override
  public synchronized void setTestDidPassIfNotFailed() {
    Assert.assertFalse(this.testDidPass);
    if (null == this.testFailureException) {
      this.testDidPass = true;
    }
    this.notifyAll();
  }

  @Override
  public synchronized void testDidFail(GalvanFailureException failureDescription) {
    // We can't fail after passing.
    Assert.assertFalse(this.testDidPass);
    // Note that it is possible to failure multiple times but we only want to store the first exception.
    if (null == this.testFailureException) {
      this.testFailureException = failureDescription;
    }
    this.notifyAll();
  }
}
