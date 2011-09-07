/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import org.apache.commons.lang.exception.ExceptionUtils;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogging;
import com.tc.test.collections.CollectionAssert;
import com.tc.text.Banner;
import com.tc.util.Assert;
import com.tc.util.EqualityComparator;
import com.tc.util.SameObjectEqualityComparator;
import com.tc.util.StandardStringifier;
import com.tc.util.Stringifier;
import com.tc.util.diff.Difference;
import com.tc.util.diff.DifferenceBuilder;
import com.tc.util.diff.Differenceable;
import com.tc.util.runtime.ThreadDump;
import com.tc.util.runtime.Vm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * A base for all Terracotta tests.
 */
public class TCTestCase extends TestCase {

  private static final long                DEFAULT_TIMEOUT_THRESHOLD = 60000;

  private final SynchronizedRef            beforeTimeoutException    = new SynchronizedRef(null);

  private DataDirectoryHelper              dataDirectoryHelper;
  private TempDirectoryHelper              tempDirectoryHelper;

  private Date                             allDisabledUntil;
  private final Map                        disabledUntil             = new Hashtable();

  // This stuff is static since Junit new()'s up an instance of the test case for each test method,
  // and the timeout covers the entire test case (ie. all methods). It wouldn't be very effective to start
  // the timer for each test method given this
  private static final Timer               timeoutTimer              = new Timer("Timeout Thread", true);
  private static final SynchronizedBoolean timeoutTaskAdded          = new SynchronizedBoolean(false);

  private static boolean                   printedProcess            = false;

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
    init();
  }

  public TCTestCase(String arg0) {
    super(arg0);
    init();
  }

  private void init() {
    TCLogging.disableLocking();
  }

  // called by timer thread (ie. NOT the main thread of test case)
  private void timeoutCallback(long elapsedTime) {
    String bar = "***************************************";
    System.err.println("\n" + bar + "\n+ TCTestCase timeout alarm going off after " + millisToMinutes(elapsedTime)
                       + " minutes at " + new Date() + "\n" + bar + "\n");
    System.err.flush();

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

  public static boolean commitTimeoutTaskAdded(boolean from, boolean to) {
    return timeoutTaskAdded.commit(from, to);
  }

  // override this method if you want to do something before your test times out
  protected void beforeTimeout() throws Throwable {
    if (false) throw new AssertionError(); // silence compiler warning
  }

  protected boolean isContainerTest() {
    return false;
  }

  protected boolean isConfiguredToRunWithAppServer() {
    return !"unknown".equals(TestConfigObject.getInstance().appServerInfo().getName());
  }

  protected boolean shouldBeSkipped() {
    return isContainerTest() ^ isConfiguredToRunWithAppServer();
  }

  @Override
  public void runBare() throws Throwable {
    printOutCurrentJavaProcesses();
    if (allDisabledUntil != null) {
      if (new Date().before(this.allDisabledUntil)) {
        System.out.println("NOTE: ALL tests in " + this.getClass().getName() + " are disabled until "
                           + this.allDisabledUntil);
        return;
      } else {
        // don't let timebomb go off on weekend
        // see INT-1173
        Calendar rightNow = Calendar.getInstance();
        int dayOfWeek = rightNow.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
          Banner.warnBanner("Timebomb is scheduled to expire on weekend (" + allDisabledUntil
                            + ". Preventing it from going off. Tests are NOT running.");
          return;
        }
        throw new Exception("Timebomb has expired on " + allDisabledUntil);
      }
    }

    final String testMethod = getName();
    System.out.println("**** Test case: " + testMethod + " ****");
    System.out.println();
    if (isTestDisabled(testMethod)) {
      System.out.println("NOTE: Test method " + testMethod + "() is disabled until "
                         + this.disabledUntil.get(testMethod));
      System.out.flush();
      return;
    }

    if (shouldBeSkipped()) {
      Banner
          .warnBanner("Test "
                      + this.getClass().getName()
                      + " is skipped because sytem test trying to run with appserver or container test running without an appserver. ");
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

  private void printOutCurrentJavaProcesses() {
    if (printedProcess) return;
    printedProcess = true;
    PrintWriter out = null;
    try {
      out = new PrintWriter(new FileWriter(this.getTempFile("javaprocesses.txt")));
      out.println(ProcessInfo.ps_grep_java());
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (out != null) out.close();
    }
  }

  public void scheduleTimeoutTask() {
    // enforce some sanity
    final int MINIMUM = 30;
    long junitTimeout = this.getTimeoutValueInSeconds();

    if (junitTimeout < MINIMUM) { throw new IllegalArgumentException("Junit timeout cannot be less than " + MINIMUM
                                                                     + " seconds"); }

    final int MIN_THRESH = 15000;
    junitTimeout *= 1000;
    if ((junitTimeout - timeoutThreshold) < MIN_THRESH) {
      System.err.println("ERROR: Cannot apply timeout threshold of " + timeoutThreshold + ", using " + MIN_THRESH
                         + " instead");
      System.err.flush();
      timeoutThreshold = MIN_THRESH;
    }

    final long delay = junitTimeout - timeoutThreshold;

    System.err.println("Timeout task is scheduled to run in " + millisToMinutes(delay) + " minutes");

    timeoutTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        timeoutCallback(delay);
      }
    }, delay);
  }

  public static void dumpHeap(File destDir) {
    if (Vm.isJDK16Compliant()) {
      try {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        String hotSpotDiagName = "com.sun.management:type=HotSpotDiagnostic";
        ObjectName name = new ObjectName(hotSpotDiagName);
        String operationName = "dumpHeap";

        File tempFile = new File(destDir, "heapDump_" + (System.currentTimeMillis()) + ".hprof");
        String dumpFilename = tempFile.getAbsolutePath();

        Object[] params = new Object[] { dumpFilename, Boolean.TRUE };
        String[] signature = new String[] { String.class.getName(), boolean.class.getName() };
        mbs.invoke(name, operationName, params, signature);

        System.out.println("dumped heap in file " + dumpFilename);
      } catch (Exception e) {
        System.err.println("Could not dump heap: " + e.getMessage());
      }
    } else {
      System.err.println("Heap dump only available on jdk1.6+");
    }
  }

  private long millisToMinutes(final long timeInMilliseconds) {
    return (timeInMilliseconds / (1000 * 60));
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

  protected synchronized TempDirectoryHelper getTempDirectoryHelper() {
    if (tempDirectoryHelper == null) {
      tempDirectoryHelper = new TempDirectoryHelper(getClass(), cleanTempDir());
    }

    return tempDirectoryHelper;
  }

  protected boolean cleanTempDir() {
    return true;
  }

  protected synchronized DataDirectoryHelper getDataDirectoryHelper() {
    if (dataDirectoryHelper == null) {
      dataDirectoryHelper = new DataDirectoryHelper(getClass());
    }

    return dataDirectoryHelper;
  }

  protected File getDataDirectory() throws IOException {
    return getDataDirectoryHelper().getDirectory();
  }

  protected File getDataFile(String fileName) throws IOException {
    return getDataDirectoryHelper().getFile(fileName);
  }

  protected File getTempDirectory() throws IOException {
    return getTempDirectoryHelper().getDirectory();
  }

  protected File getTempFile(String fileName) throws IOException {
    return getTempDirectoryHelper().getFile(fileName);
  }

  /**
   * Disable ALL tests until the given date. This method should be called in the constructor of your unit test
   */
  protected final void disableAllUntil(Date theDate) {
    Assert.eval(theDate != null);
    if (allDisabledUntil == null || allDisabledUntil.before(theDate)) {
      allDisabledUntil = theDate;
    }
    Banner.warnBanner(this.getClass().getName() + " disabled until " + allDisabledUntil);
  }

  /**
   * Disable ALL tests until the given date. This method should be called in the constructor of your unit test
   */
  protected final void disableAllUntil(String theDate) {
    disableAllUntil(parseDate(theDate));
  }

  /**
   * Disable ALL tests until the given date. This method should be called in the constructor of your unit test Only
   * specified platforms ("windows", "linux", "solaris") will take effect.
   */
  protected final void disableAllUntil(String theDate, String[] platforms) {
    List platform_list = Arrays.asList(platforms);
    String platform = TestConfigObject.getInstance().platform();
    if (platform_list.contains(platform)) {
      disableAllUntil(parseDate(theDate));
    }
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
   * test. Only specified platforms ("windows", "linux", "solaris") take effect.
   */
  protected final void disableTestUntil(String method, String date, String[] platforms) {
    List platform_list = Arrays.asList(platforms);
    String platform = TestConfigObject.getInstance().platform();
    if (platform_list.contains(platform)) {
      this.disabledUntil.put(method, parseDate(date));
    }
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
      DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
      format.setLenient(false);
      return format.parse(date);
    } catch (ParseException e) {
      // throwing runtime exception should cause each test case to fail
      // (provided you're disabling from the constructor
      // as directed)
      throw new TCRuntimeException(e);
    }
  }

  protected final boolean isAllDisabled() {
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
      assertEquals("Object and [de]serialized object failed hashCode() comparison", obj.hashCode(),
                   deserializedObj.hashCode());
    }
  }

  protected synchronized void assertTimeDirection() {
    long currentMillis = System.currentTimeMillis();
    assertTrue("System Clock Moved Backwards! [current=" + currentMillis + ", previous=" + previousSystemMillis + "]",
               currentMillis >= previousSystemMillis);
    previousSystemMillis = currentMillis;
  }

  private void doThreadDump() {
    ThreadDump.dumpAllJavaProcesses(numThreadDumps, dumpInterval);
  }

  /**
   * Returns the timeout value
   */
  public int getTimeoutValueInSeconds() {
    return TestConfigObject.getInstance().getJunitTimeoutInSeconds();
  }

  protected int getThreadDumpCount() {
    return numThreadDumps;
  }

  protected long getThreadDumpInterval() {
    return dumpInterval;
  }
}
