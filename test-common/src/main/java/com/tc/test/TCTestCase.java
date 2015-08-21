/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import junit.framework.TestCase;

import com.tc.exception.TCRuntimeException;
import com.tc.util.Assert;
import com.tc.util.Banner;
import com.tc.util.runtime.ThreadDump;

/**
 * A base for all Terracotta tests.
 */
public class TCTestCase extends TestCase {

  private static final long                DEFAULT_TIMEOUT_THRESHOLD = 60000;

  private final AtomicReference<Throwable> beforeTimeoutException    = new AtomicReference<>(null);

  private DataDirectoryHelper              dataDirectoryHelper;
  private TempDirectoryHelper              tempDirectoryHelper;

  protected Date                           allDisabledUntil;

  // This stuff is static since Junit new()'s up an instance of the test case for each test method,
  // and the timeout covers the entire test case (ie. all methods). It wouldn't be very effective to start
  // the timer for each test method given this
  private static final Timer               timeoutTimer              = new Timer("Timeout Thread", true);
  private static TimerTask                 timerTask;
  protected static final AtomicBoolean     timeoutTaskAdded          = new AtomicBoolean(false);

  // If you want to customize this, you have to do it in the constructor of your test case (setUp() is too late)
  private long                             timeoutThreshold          = DEFAULT_TIMEOUT_THRESHOLD;

  // controls for thread dumping.
  private boolean                          dumpThreadsOnTimeout      = true;
  private int                              numThreadDumps            = 3;
  private long                             dumpInterval              = 500;

  // a way to ensure that system clock moves forward...
  private long                             previousSystemMillis      = 0;

  protected volatile boolean               testWillRun               = false;

  public TCTestCase() {
    super();
    init();
  }

  public TCTestCase(String arg0) {
    super(arg0);
    init();
  }

  private void init() {
    //TCLogging.disableLocking();
  }

  // called by timer thread (ie. NOT the main thread of test case)
  private void timeoutCallback(long elapsedTime) {
    String bar = "***************************************";
    System.err
        .println("\n" + bar + "\n+ TCTestCase timeout alarm going off after "
                 + TimeUnit.MILLISECONDS.toMinutes(elapsedTime) + " minutes at " + new Date() + "\n" + bar + "\n");
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
    return timeoutTaskAdded.compareAndSet(from, to);
  }

  // override this method if you want to do something before your test times out
  protected void beforeTimeout() throws Throwable {
    //
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

  protected void tcTestCaseSetup() throws Exception {
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
    if (timeoutTaskAdded.compareAndSet(false, true)) {
      scheduleTimeoutTask();
    }

    // this should be the last thing happening indicating this test case will actually execute
    testWillRun = true;
  }

  @Override
  public void runBare() throws Throwable {
    tcTestCaseSetup();
    if (!testWillRun) return;

    Throwable testException = null;
    try {
      super.runBare();
    } catch (Throwable t) {
      testException = t;
    }

    tcTestCaseTearDown(testException);
  }

  protected void tcTestCaseTearDown(Throwable testException) throws Throwable {
    cancelTimeoutTask();
    Throwable exceptionInTimeoutCallback = beforeTimeoutException.get();

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

  public synchronized void scheduleTimeoutTask() {
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

    System.err.println("Timeout task is scheduled to run in " + TimeUnit.MILLISECONDS.toMinutes(delay) + " minutes");

    // cancel the old task
    if (timerTask != null) {
      timerTask.cancel();
    }
    final Thread testVMThread = Thread.currentThread();
    timerTask = new TimerTask() {

      @Override
      public void run() {
        timeoutCallback(delay);
        // DEV-8901 interrupt the test VM thread so that if its waiting somewhere, It comes out and the test vm exits.
        testVMThread.interrupt();
      }
    };
    timeoutTimer.schedule(timerTask, delay);
  }

  private void cancelTimeoutTask() {
    if (timeoutTaskAdded.compareAndSet(true, false)) {
      if (timerTask != null) {
        timerTask.cancel();
      }
    }
  }

  public static void dumpHeap(File destDir) {
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
  private final void disableAllUntil(Date theDate) {
    Assert.eval(theDate != null);
    if (allDisabledUntil == null || allDisabledUntil.before(theDate)) {
      allDisabledUntil = theDate;
    }
    Banner.warnBanner(this.getClass().getName() + " disabled until " + allDisabledUntil);
  }

  /**
   * Disable ALL tests until the given date. This method should be called in the constructor of your unit test
   */
  protected final void timebombTest(String date) {
    disableAllUntil(parseDate(date));
  }

  /**
   * Disable all tests indefinitely
   */
  protected final void disableTest() {
    disableAllUntil(new Date(Long.MAX_VALUE));
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
  
  protected void assertEqualsUnordered(Object[] a1, Object[] a2) {
    if (a1.length != a2.length) {
      throw new AssertionError(Arrays.asList(a1) + " != " + Arrays.asList(a2));
    }
    
    List<Object> asList = Arrays.asList(a2);
    
    for (Object o : a1) {
      if (! asList.contains(o)) {
        throw new AssertionError(Arrays.asList(a1) + " != " + Arrays.asList(a2));
      }
    }
  }

  protected final void assertContains(String message, String expected, String actual) {
    if ((expected == null) != (actual == null)) {
      message = (message == null ? "" : message + ": ");
      TestCase.fail(message + "Expected was " + (expected == null ? "<null>" : "'" + expected + "'") + ", but actual was "
                    + (actual == null ? "<null>" : "'" + actual + "'"));
    }

    if (expected != null) {
      if (actual.indexOf(expected) < 0) {
        message = (message == null ? "" : message + ": ");
        TestCase.fail(message + "Actual string '" + actual + "' does not contain expected string '" + expected + "'");
      }
    }
  }

  protected final void fail(Throwable t) {
    fail("FAILURE", t);
  }

  protected final void fail(String message, Throwable t) {
    throw new AssertionError(message == null ? "" : (message + "\n"), t);
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

  protected void checkComparator(Object smaller, Object bigger, Object equalToBigger, Comparator<Object> c) {
    // test null's
    TestCase.assertTrue(c.compare(null, bigger) < 0);
    TestCase.assertTrue(c.compare(bigger, null) > 0);
    TestCase.assertTrue(c.compare(null, null) == 0);

    // test less-than
    TestCase.assertTrue(c.compare(smaller, bigger) < 0);

    // test greater-than
    TestCase.assertTrue(c.compare(bigger, smaller) > 0);

    // test equal
    TestCase.assertTrue(c.compare(bigger, equalToBigger) == 0);
    TestCase.assertTrue(c.compare(equalToBigger, bigger) == 0);
  }

  protected void assertNotEquals(int i1, int i2) {
    TestCase.assertFalse("Values are equal: " + i1, i1 == i2);
  }

  protected void assertEquals(byte[] b1, byte[] b2) {
    boolean rv = (b1 == null) ? b2 == null : Arrays.equals(b1, b2);
    TestCase.assertTrue("Values are not equals", rv);
  }

  protected void assertNotEquals(Object o1, Object o2) {
    TestCase.assertFalse("Values are equal: " + o1 + ", " + o2, o1 == o2);
    if (o1 != null && o2 != null) {
      TestCase.assertFalse("Values are equal: " + o1 + ", " + o2, o1.equals(o2));
      TestCase.assertFalse("Values are equal: " + o1 + ", " + o2, o2.equals(o1));
    }
  }

  protected void assertSerializable(Object obj) {
    assertSerializable(obj, true, true);
  }

  protected void assertSerializable(Object obj, boolean checkEquals, boolean checkHashCode) {
    TestCase.assertNotNull(obj);
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
    TestCase.assertNotNull(obj);
    if (checkEquals) {
      TestCase.assertEquals("Object and [de]serialized object failed equals() comparison", obj, deserializedObj);
    }
    if (checkHashCode) {
      TestCase.assertEquals("Object and [de]serialized object failed hashCode() comparison", obj.hashCode(),
          deserializedObj.hashCode());
    }
  }

  protected synchronized void assertTimeDirection() {
    long currentMillis = System.currentTimeMillis();
    TestCase.assertTrue("System Clock Moved Backwards! [current=" + currentMillis + ", previous=" + previousSystemMillis + "]",
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
