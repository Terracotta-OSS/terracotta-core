/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.tests.base;

import java.util.Calendar;
import java.util.Date;

import com.tc.test.TCTestCase;
import com.tc.text.Banner;

/**
 * A base for all Terracotta tests.
 */
public class TerracottaTestCase extends TCTestCase {

  public TerracottaTestCase() {
    super();
  }

  public TerracottaTestCase(String arg0) {
    super(arg0);
  }


  protected boolean preRun() throws Exception {
    printOutCurrentJavaProcesses();
    if (allDisabledUntil != null) {
      if (new Date().before(this.allDisabledUntil)) {
        System.out.println("NOTE: ALL tests in " + this.getClass().getName() + " are disabled until "
                           + this.allDisabledUntil);
        return false;
      } else {
        // don't let timebomb go off on weekend
        // see INT-1173
        Calendar rightNow = Calendar.getInstance();
        int dayOfWeek = rightNow.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
          Banner.warnBanner("Timebomb is scheduled to expire on weekend (" + allDisabledUntil
                            + ". Preventing it from going off. Tests are NOT running.");
          return false;
        }
        throw new Exception("Timebomb has expired on " + allDisabledUntil);
      }
    }

    if (!shouldTestRunInCurrentExecutionMode()) { return false; }

    if (shouldBeSkipped()) {
      Banner
          .warnBanner("Test "
                      + this.getClass().getName()
                      + " is skipped because sytem test trying to run with appserver or container test running without an appserver. ");
      return false;
    }

    // don't move this stuff to runTest(), you want the timeout timer to catch hangs in setUp() too.
    // Yes it means you can't customize the timeout threshold in setUp() -- take a deep breath and
    // set your value in the constructor of your test case instead of setUp()
    if (timeoutTaskAdded.commit(false, true)) {
      scheduleTimeoutTask();
    }

    // no errors -- woo-hoo!
    return true;
  }

  @Override
  public void runBare() throws Throwable {
    // do nothing 
  }

}
