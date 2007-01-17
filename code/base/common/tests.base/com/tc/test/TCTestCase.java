/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import org.apache.commons.lang.exception.ExceptionUtils;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogging;
import com.tc.test.collections.CollectionAssert;
import com.tc.util.Assert;
import com.tc.util.EqualityComparator;
import com.tc.util.SameObjectEqualityComparator;
import com.tc.util.StandardStringifier;
import com.tc.util.Stringifier;
import com.tc.util.TCTimerImpl;
import com.tc.util.diff.Difference;
import com.tc.util.diff.DifferenceBuilder;
import com.tc.util.diff.Differenceable;
import com.tc.util.runtime.ThreadDump;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * A base for all Terracotta tests.
 */
public class TCTestCase extends TestCase {

  private static final long                DEFAULT_TIMEOUT_THRESHOLD = 60000;
  private static final DateFormat          DATE_FORMAT               = new SimpleDateFormat("yyyy-MM-dd");

  private final SynchronizedRef            beforeTimeoutException    = new SynchronizedRef(null);

  private DataDirectoryHelper              dataDirectoryHelper;
  private TempDirectoryHelper              tempDirectoryHelper;

  private Date                             allDisabledUntil;
  private final Map                        disabledUntil             = new Hashtable();

  // This stuff is static since Junit new()'s up an instance of the test case for each test method,
  // and the timeout covers the entire test case (ie. all methods). It wouldn't be very effective to start
  // the timer for each test method given this
  private static final Timer               timeoutTimer              = new TCTimerImpl("Timeout Thread", true);
  private static final SynchronizedBoolean timeoutTaskAdded          = new SynchronizedBoolean(false);

  // If you want to customize this, you have to do it in the constructor of your test case (setUp() is too late)
  private long                             timeoutThreshold          = DEFAULT_TIMEOUT_THRESHOLD;

  // controls for thread dumping.
  private boolean                          dumpThreadsOnTimeout      = true;
  private int                              numThreadDumps            = 3;
  private long                             dumpInterval              = 500;

  // a way to ensure that system clock moves forward...
  private long                             previousSystemMillis      = 0;

  public TCTestCase() {
    super();

    TCLogging.disableLocking();
  }

  public TCTestCase(String arg0) {
    super(arg0);

    TCLogging.disableLocking();
  }

  // called by timer thread (ie. NOT the main thread of test case)
  private void timeoutCallback() {
    String bar = "***************************************";
    System.err.println("\n" + bar + "\n+ TCTestCase timeout alarm going off at " + new Date() + "\n" + bar + "\n");

    doDumpServerDetails();
    if (dumpThreadsOnTimeout) {
      try {
        doThreadDump();
      } catch (Throwable t) {
        // don't fail the test b/c of this
        t.printStackTrace();
      }
    }

    try {
      beforeTimeout();
    } catch (Throwable t) {
      this.beforeTimeoutException.set(t);
    }
  }

  protected void doDumpServerDetails() {
    // NOP - Overridden by subclasses
  }

  // override this method if you want to do something before your test times out
  protected void beforeTimeout() throws Throwable {
    if (false) throw new AssertionError(); // silence compiler warning
  }

  public void runBare() throws Throwable {
    if (isAllDisabled()) {
      System.out.println("NOTE: ALL tests in " + this.getClass().getName() + " are disabled until "
                         + this.allDisabledUntil);
      return;
    }

    final String testMethod = getName();
    if (isTestDisabled(testMethod)) {
      System.out.println("NOTE: Test method " + testMethod + "() is disabled until "
                         + this.disabledUntil.get(testMethod));
      return;
    }

    // don't move this stuff to runTest(), you want the timeout timer to catch hangs in setUp() too.
    // Yes it means you can't customize the timeout threshold in setUp() -- take a deep breath and
    // set your value in the constructor of your test case instead of setUp()
    if (timeoutTaskAdded.commit(false, true)) {
      scheduleTimeoutTask();
    }

    Throwable testException = null;
    try {
      super.runBare();
    } catch (Throwable t) {
      testException = t;
    }

    Throwable exceptionInTimeoutCallback = (Throwable) beforeTimeoutException.get();

    // favor the "real" exception to make test fail. If there was a exception in the timeout callback,
    // make that able to fail the test too
    if (testException != null) {
      if (exceptionInTimeoutCallback != null) {
        exceptionInTimeoutCallback.printStackTrace();
      }
      throw testException;
    }

    if (exceptionInTimeoutCallback != null) { throw exceptionInTimeoutCallback; }

    // no errors -- woo-hoo!
    return;
  }

  private void scheduleTimeoutTask() throws IOException {
    // enforce some sanity
    final int MINIMUM = 30;
    long junitTimeout = TestConfigObject.getInstance().getJunitTimeoutInSeconds();

    if (junitTimeout < MINIMUM) { throw new IllegalArgumentException("Junit timeout cannot be less than " + MINIMUM
                                                                     + " seconds"); }

    final int MIN_THRESH = 15000;
    junitTimeout *= 1000;
    if ((junitTimeout - timeoutThreshold) < MIN_THRESH) {
      System.err.println("ERROR: Cannot apply timeout threshold of " + timeoutThreshold + ", using " + MIN_THRESH
                         + " instead");
      timeoutThreshold = MIN_THRESH;
    }

    long delay = junitTimeout - timeoutThreshold;

    timeoutTimer.schedule(new TimerTask() {
      public void run() {
        timeoutCallback();
      }
    }, delay);
  }

  public void setThreadDumpInterval(long interval) {
    this.dumpInterval = interval;
  }

  public void setDumpThreadsOnTimeout(boolean dump) {
    this.dumpThreadsOnTimeout = dump;
  }

  public void setNumThreadDumps(int dumps) {
    this.numThreadDumps = dumps;
  }

  public void setTimeoutThreshold(long threshold) {
    this.timeoutThreshold = threshold;
  }

  protected final synchronized TempDirectoryHelper getTempDirectoryHelper() {
    if (this.tempDirectoryHelper == null) {
      try {
        this.tempDirectoryHelper = new TempDirectoryHelper(getClass(), cleanTempDir());
      } catch (IOException ioe) {
        throw new TCRuntimeException("Can't get configuration for temp directory", ioe);
      }
    }

    return this.tempDirectoryHelper;
  }

  protected boolean cleanTempDir() {
    return true;
  }

  protected final synchronized DataDirectoryHelper getDataDirectoryHelper() {
    if (this.dataDirectoryHelper == null) {
      try {
        this.dataDirectoryHelper = new DataDirectoryHelper(getClass());
      } catch (IOException ioe) {
        throw new TCRuntimeException(ioe.getLocalizedMessage(), ioe);
      }
    }

    return this.dataDirectoryHelper;
  }

  protected final void cleanTempDirectory() throws IOException {
    getTempDirectoryHelper().cleanTempDirectory();
  }

  protected final File getDataDirectory() throws IOException {
    return getDataDirectoryHelper().getDirectory();
  }

  protected final File getDataFile(String fileName) throws IOException {
    return getDataDirectoryHelper().getFile(fileName);
  }

  protected final File getTempDirectory() throws IOException {
    return getTempDirectoryHelper().getDirectory();
  }

  protected final File getTempFile(String fileName) throws IOException {
    return getTempDirectoryHelper().getFile(fileName);
  }

  /**
   * Disable ALL tests until the given date. This method should be called in the constructor of your unit test
   */
  protected final void disableAllUntil(Date theDate) {
    Assert.eval(theDate != null);
    this.allDisabledUntil = theDate;
  }

  /**
   * Disable ALL tests until the given date. This method should be called in the constructor of your unit test
   */
  protected final void disableAllUntil(String theDate) {
    disableAllUntil(parseDate(theDate));
  }

  /**
   * Disable the given test method until the given date. This method should be called in the constructor of your unit
   * test
   */
  protected final void disableTestUntil(String method, String date) {
    this.disabledUntil.put(method, parseDate(date));
  }

  /**
   * Disable the given test method until the given date. This method should be called in the constructor of your unit
   * test
   */
  protected final void disableTestUntil(String method, Date date) {
    this.disabledUntil.put(method, date);
  }

  protected final void assertSameOrdered(Object one, Object two) {
    assertEqualsOrdered(one, two, SameObjectEqualityComparator.INSTANCE);
  }

  protected final void assertEqualsOrdered(Object one, Object two) {
    CollectionAssert.assertEqualOrdered(one, two);
  }

  protected final void assertEqualsOrdered(Object one, Object two, EqualityComparator comparator) {
    CollectionAssert.assertEqualOrdered(one, two, comparator);
  }

  protected final void assertSameUnordered(Object one, Object two) {
    assertEqualsUnordered(one, two, SameObjectEqualityComparator.INSTANCE);
  }

  protected final void assertEqualsUnordered(Object one, Object two) {
    CollectionAssert.assertEqualUnordered(one, two);
  }

  protected final void assertEqualsUnordered(Object one, Object two, EqualityComparator comparator) {
    CollectionAssert.assertEqualUnordered(one, two, comparator);
  }

  protected final void assertSameUnorderedUncounted(Object one, Object two) {
    assertEqualsUnorderedUncounted(one, two, SameObjectEqualityComparator.INSTANCE);
  }

  protected final void assertEqualsUnorderedUncounted(Object one, Object two) {
    CollectionAssert.assertEqualUnorderedUncounted(one, two);
  }

  protected final void assertEqualsUnorderedUncounted(Object one, Object two, EqualityComparator comparator) {
    CollectionAssert.assertEqualUnorderedUncounted(one, two, comparator);
  }

  protected final void assertEqualsVerbose(Object one, Object two) {
    assertEqualsVerbose(null, one, two, StandardStringifier.INSTANCE, false);
  }

  protected final void assertEqualsVerbose(Object one, Object two, Stringifier stringifier) {
    assertEqualsVerbose(null, one, two, stringifier, false);
  }

  protected final void assertEqualsVerbose(Object one, Object two, boolean showObjects) {
    assertEqualsVerbose(null, one, two, StandardStringifier.INSTANCE, showObjects);
  }

  protected final void assertEqualsVerbose(String message, Object one, Object two) {
    assertEqualsVerbose(message, one, two, StandardStringifier.INSTANCE, false);
  }

  protected final void assertEqualsVerbose(String message, Object one, Object two, Stringifier stringifier) {
    assertEqualsVerbose(message, one, two, stringifier, false);
  }

  protected final void assertEqualsVerbose(String message, Object one, Object two, boolean showObjects) {
    assertEqualsVerbose(message, one, two, StandardStringifier.INSTANCE, showObjects);
  }

  protected final void assertContainsIgnoreCase(String expected, String actual) {
    assertContainsIgnoreCase(null, expected, actual);
  }

  protected final void assertContainsIgnoreCase(String message, String expected, String actual) {
    assertContains(message, expected != null ? expected.toLowerCase() : null, actual != null ? actual.toLowerCase()
        : null);
  }

  protected final void assertContains(String expected, String actual) {
    assertContains(null, expected, actual);
  }

  protected final void assertContains(String message, String expected, String actual) {
    if ((expected == null) != (actual == null)) {
      message = (message == null ? "" : message + ": ");
      fail(message + "Expected was " + (expected == null ? "<null>" : "'" + expected + "'") + ", but actual was "
           + (actual == null ? "<null>" : "'" + actual + "'"));
    }

    if (expected != null) {
      if (actual.indexOf(expected) < 0) {
        message = (message == null ? "" : message + ": ");
        fail(message + "Actual string '" + actual + "' does not contain expected string '" + expected + "'");
      }
    }
  }

  protected final void assertEqualsVerbose(String message, Object one, Object two, Stringifier stringifier,
                                           boolean showObjects) {
    if (one != null && two != null && (one instanceof Differenceable) && (two instanceof Differenceable)
        && (one.getClass().equals(two.getClass())) && (!one.equals(two))) {
      Difference[] differences = DifferenceBuilder.getDifferencesAsArray((Differenceable) one, (Differenceable) two);
      Assert.eval(differences.length > 0); // since we know they're not equal

      StringBuffer descrip = new StringBuffer();
      descrip.append((message != null ? (message + ": ") : "") + "objects not equal");
      descrip.append(DifferenceBuilder.describeDifferences((Differenceable) one, (Differenceable) two));

      if (showObjects) {
        descrip.append("\nexpected:\n");
        descrip.append(one.toString());
        descrip.append("\nbut was:\n");
        descrip.append(two.toString());
        descrip.append("\n");
      }

      throw new AssertionFailedError(descrip.toString());
    } else {
      assertEquals(one, two);
    }
  }

  protected final void fail(Throwable t) {
    fail("FAILURE", t);
  }

  protected final void fail(String message, Throwable t) {
    fail((message == null ? "" : (message + "\n")) + "Exception:\n" + ExceptionUtils.getFullStackTrace(t));
  }

  private Date parseDate(String date) {
    try {
      return DATE_FORMAT.parse(date);
    } catch (ParseException e) {
      // throwing runtime exception should cause each test case to fail
      // (provided you're disabling from the constructor
      // as directed)
      throw new TCRuntimeException(e);
    }
  }

  private boolean isAllDisabled() {
    return this.allDisabledUntil != null && new Date().before(this.allDisabledUntil);
  }

  private boolean isTestDisabled(String testMethod) {
    Date until = (Date) disabledUntil.get(testMethod);
    return until != null && new Date().before(until);
  }

  protected void checkComparator(Object smaller, Object bigger, Object equalToBigger, Comparator c) {
    // test null's
    assertTrue(c.compare(null, bigger) < 0);
    assertTrue(c.compare(bigger, null) > 0);
    assertTrue(c.compare(null, null) == 0);

    // test less-than
    assertTrue(c.compare(smaller, bigger) < 0);

    // test greater-than
    assertTrue(c.compare(bigger, smaller) > 0);

    // test equal
    assertTrue(c.compare(bigger, equalToBigger) == 0);
    assertTrue(c.compare(equalToBigger, bigger) == 0);
  }

  protected void assertNotEquals(int i1, int i2) {
    assertFalse("Values are equal: " + i1, i1 == i2);
  }

  protected void assertEquals(byte[] b1, byte[] b2) {
    boolean rv = (b1 == null) ? b2 == null : Arrays.equals(b1, b2);
    assertTrue("Values are not equals", rv);
  }

  protected void assertNotEquals(Object o1, Object o2) {
    assertFalse("Values are equal: " + o1 + ", " + o2, o1 == o2);
    if (o1 != null && o2 != null) {
      assertFalse("Values are equal: " + o1 + ", " + o2, o1.equals(o2));
      assertFalse("Values are equal: " + o1 + ", " + o2, o2.equals(o1));
    }
  }

  protected void assertSerializable(Object obj) {
    assertSerializable(obj, true, true);
  }

  protected void assertSerializable(Object obj, boolean checkEquals, boolean checkHashCode) {
    assertNotNull(obj);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos;
    Object deserializedObj = null;
    try {
      oos = new ObjectOutputStream(baos);
      oos.writeObject(obj);
      oos.close();
      ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
      deserializedObj = ois.readObject();
    } catch (IOException ioe) {
      throw Assert.failure("Object failed to serialize", ioe);
    } catch (ClassNotFoundException cnfe) {
      throw Assert.failure("Object failed to serialize", cnfe);
    }
    assertNotNull(obj);
    if (checkEquals) {
      assertEquals("Object and [de]serialized object failed equals() comparison", obj, deserializedObj);
    }
    if (checkHashCode) {
      assertEquals("Object and [de]serialized object failed hashCode() comparison", obj.hashCode(), deserializedObj
          .hashCode());
    }
  }

  protected synchronized void assertTimeDirection() {
    long currentMillis = System.currentTimeMillis();
    assertTrue("System Clock Moved Backwards! [current=" + currentMillis + ", previous=" + previousSystemMillis + "]",
               currentMillis >= previousSystemMillis);
    previousSystemMillis = currentMillis;
  }

  private void doThreadDump() {
    ThreadDump.dumpThreadsMany(numThreadDumps, dumpInterval);
  }

}
