/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.statistics.buffer.h2;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticsSystemType;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.StatisticsBufferListener;
import com.tc.statistics.buffer.StatisticsConsumer;
import com.tc.statistics.buffer.exceptions.StatisticsBufferException;
import com.tc.statistics.buffer.h2.H2StatisticsBufferImpl;
import com.tc.statistics.config.impl.StatisticsConfigImpl;
import com.tc.statistics.database.exceptions.StatisticsDatabaseNotReadyException;
import com.tc.statistics.database.exceptions.StatisticsDatabaseStructureFuturedatedError;
import com.tc.statistics.database.exceptions.StatisticsDatabaseStructureOutdatedError;
import com.tc.statistics.database.impl.H2StatisticsDatabase;
import com.tc.statistics.jdbc.JdbcHelper;
import com.tc.statistics.retrieval.StatisticsRetriever;
import com.tc.test.TempDirectoryHelper;
import com.tc.util.TCAssertionError;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Random;

import junit.framework.TestCase;

public class H2StatisticsBufferTest extends TestCase {
  private StatisticsBuffer buffer;
  private File             tmpDir;

  private final Random     random = new Random();

  @Override
  public void setUp() throws Exception {
    tmpDir = createTmpDir();
    buffer = new H2StatisticsBufferImpl(StatisticsSystemType.CLIENT, new StatisticsConfigImpl(), tmpDir);
    buffer.open();
    buffer.setDefaultAgentDifferentiator("L2");
  }

  private File createTmpDir() throws IOException {
    File tmp_dir_parent = new TempDirectoryHelper(getClass(), false).getDirectory();
    File tmp_dir;
    synchronized (random) {
      tmp_dir = new File(tmp_dir_parent, "statisticsbuffer-" + random.nextInt() + "-" + System.currentTimeMillis());
    }
    tmp_dir.mkdirs();
    return tmp_dir;
  }

  @Override
  public void tearDown() throws Exception {
    buffer.close();
  }

  public void testInvalidBufferDirectory() throws Exception {
    try {
      new H2StatisticsBufferImpl(StatisticsSystemType.CLIENT, new StatisticsConfigImpl(), null);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // dir can't be null
    }

    File tmp_dir = createTmpDir();
    tmp_dir.delete();
    try {
      new H2StatisticsBufferImpl(StatisticsSystemType.CLIENT, new StatisticsConfigImpl(), tmp_dir);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // dir doesn't exist
    }

    tmp_dir = createTmpDir();
    tmp_dir.delete();
    tmp_dir.createNewFile();
    try {
      new H2StatisticsBufferImpl(StatisticsSystemType.CLIENT, new StatisticsConfigImpl(), tmp_dir);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // path is not a dir
    } finally {
      tmp_dir.delete();
    }

    tmp_dir = createTmpDir();
    boolean tmpDirReadOnly = tmp_dir.setReadOnly();

    if (tmp_dir.canWrite()) {
      System.err
          .println("XXX "
                   + tmp_dir.getAbsolutePath()
                   + " though set readonly is still writable. Below expected assertion error will not happen in tht case. Java/OS issue.");
      return;
    }

    if (tmpDirReadOnly) {
      try {
        new H2StatisticsBufferImpl(StatisticsSystemType.CLIENT, new StatisticsConfigImpl(), tmp_dir);
        fail("expected exception");
      } catch (TCAssertionError e) {
        // dir is not writable
      } finally {
        tmp_dir.delete();
      }
    }
  }

  public void testOutdatedVersionCheck() throws Exception {
    buffer.close();

    H2StatisticsDatabase database = new H2StatisticsDatabase(tmpDir, H2StatisticsBufferImpl.H2_URL_SUFFIX);
    database.open();
    try {
      JdbcHelper.executeUpdate(database.getConnection(), "UPDATE dbstructureversion SET version = "
                                                         + (H2StatisticsBufferImpl.DATABASE_STRUCTURE_VERSION - 1));
      try {
        buffer.open();
        fail("expected exception");
      } catch (StatisticsDatabaseStructureOutdatedError e) {
        assertEquals(H2StatisticsBufferImpl.DATABASE_STRUCTURE_VERSION - 1, e.getActualVersion());
        assertEquals(H2StatisticsBufferImpl.DATABASE_STRUCTURE_VERSION, e.getExpectedVersion());
        assertNotNull(e.getCreationDate());
      }
    } finally {
      database.close();
    }
  }

  public void testFuturedatedVersionCheck() throws Exception {
    buffer.close();

    H2StatisticsDatabase database = new H2StatisticsDatabase(tmpDir, H2StatisticsBufferImpl.H2_URL_SUFFIX);
    database.open();
    try {
      JdbcHelper.executeUpdate(database.getConnection(), "UPDATE dbstructureversion SET version = "
                                                         + (H2StatisticsBufferImpl.DATABASE_STRUCTURE_VERSION + 1));
      try {
        buffer.open();
        fail("expected exception");
      } catch (StatisticsDatabaseStructureFuturedatedError e) {
        assertEquals(H2StatisticsBufferImpl.DATABASE_STRUCTURE_VERSION + 1, e.getActualVersion());
        assertEquals(H2StatisticsBufferImpl.DATABASE_STRUCTURE_VERSION, e.getExpectedVersion());
        assertNotNull(e.getCreationDate());
      }
    } finally {
      database.close();
    }
  }

  public void testOpenClose() throws Exception {
    // several opens and closes are silently detected
    buffer.open();
    buffer.open();
    buffer.close();
    buffer.close();
  }

  public void testCloseUnopenedBuffer() throws Exception {
    buffer.close();

    File tmp_dir = createTmpDir();
    StatisticsBuffer newBuffer = new H2StatisticsBufferImpl(StatisticsSystemType.CLIENT, new StatisticsConfigImpl(),
                                                            tmp_dir);
    newBuffer.close(); // should not throw an exception
  }

  public void testCreateCaptureSessionUnopenedBuffer() throws Exception {
    buffer.close();
    try {
      buffer.createCaptureSession("theid");
      fail("expected exception");
    } catch (StatisticsBufferException e) {
      // expected
      assertTrue(e.getCause() instanceof StatisticsDatabaseNotReadyException);
    }
  }

  public void testCreateCaptureSession() throws Exception {
    StatisticsRetriever retriever1 = buffer.createCaptureSession("theid1");
    assertNotNull(retriever1);
    assertEquals("theid1", retriever1.getSessionId());

    StatisticsRetriever retriever2 = buffer.createCaptureSession("theid2");
    assertNotNull(retriever2);
    assertEquals("theid2", retriever2.getSessionId());

    StatisticsRetriever retriever3 = buffer.createCaptureSession("theid3");
    assertNotNull(retriever3);
    assertEquals("theid3", retriever3.getSessionId());
  }

  public void testCreateCaptureSessionNotUnique() throws Exception {
    buffer.createCaptureSession("theid1");
    buffer.createCaptureSession("theid1");
  }

  public void testStoreStatisticsDataNullSessionId() throws Exception {
    try {
      buffer.storeStatistic(new StatisticData().agentIp(InetAddress.getLocalHost().getHostAddress())
          .agentDifferentiator("L1/0").moment(new Date()).name("name"));
      fail("expected exception");
    } catch (NullPointerException e) {
      // sessionId can't be null
    }
  }

  public void testStoreStatisticsDataNullAgentIp() throws Exception {
    buffer.createCaptureSession("someid");
    buffer.storeStatistic(new StatisticData().sessionId("someid").agentDifferentiator("L1/0").moment(new Date())
        .name("name"));
    buffer.setDefaultAgentIp(null);
    try {
      buffer.storeStatistic(new StatisticData().sessionId("someid").agentDifferentiator("L1/0").moment(new Date())
          .name("name"));
      fail("expected exception");
    } catch (NullPointerException e) {
      // agentIp can't be null
    }
  }

  public void testStoreStatisticsDataNullAgentDifferentiator() throws Exception {
    buffer.createCaptureSession("someid");
    buffer.storeStatistic(new StatisticData().sessionId("someid").agentIp(InetAddress.getLocalHost().getHostAddress())
        .moment(new Date()).name("name"));
    buffer.setDefaultAgentDifferentiator(null);
    try {
      buffer.storeStatistic(new StatisticData().sessionId("someid")
          .agentIp(InetAddress.getLocalHost().getHostAddress()).moment(new Date()).name("name"));
      fail("expected exception");
    } catch (NullPointerException e) {
      // agentDifferentiator can't be null
    }
  }

  public void testStoreStatisticsDataNullMoment() throws Exception {
    buffer.createCaptureSession("someid");
    try {
      buffer.storeStatistic(new StatisticData().sessionId("someid")
          .agentIp(InetAddress.getLocalHost().getHostAddress()).agentDifferentiator("L1/0").name("name").data("test"));
      fail("expected exception");
    } catch (NullPointerException e) {
      // moment can't be null
    }
  }

  public void testStoreStatisticsDataNullName() throws Exception {
    buffer.createCaptureSession("someid");
    try {
      buffer.storeStatistic(new StatisticData().sessionId("someid")
          .agentIp(InetAddress.getLocalHost().getHostAddress()).agentDifferentiator("L1/0").moment(new Date())
          .data("test"));
      fail("expected exception");
    } catch (NullPointerException e) {
      // name can't be null
    }
  }

  public void testStoreStatisticsDataNullData() throws Exception {
    buffer.createCaptureSession("someid");
    buffer.storeStatistic(new StatisticData().sessionId("someid").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("L1/0").moment(new Date()).name("name"));
  }

  public void testStoreStatisticsUnopenedBuffer() throws Exception {
    buffer.createCaptureSession("someid");

    buffer.close();
    try {
      buffer.storeStatistic(new StatisticData().sessionId("someid")
          .agentIp(InetAddress.getLocalHost().getHostAddress()).agentDifferentiator("L1/0").moment(new Date())
          .name("name").data("test"));
      fail("expected exception");
    } catch (StatisticsBufferException e) {
      // expected
      assertTrue(e.getCause() instanceof StatisticsDatabaseNotReadyException);
    }
  }

  public void testReinitialize() throws Exception {
    buffer.createCaptureSession("someid1");

    buffer.storeStatistic(new StatisticData().sessionId("someid1").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("L1/0").agentDifferentiator("yummy").moment(new Date()).name("the stat").data("stuff"));

    buffer.reinitialize();

    buffer.createCaptureSession("someid1");
    final int[] count = new int[] { 0 };
    buffer.consumeStatistics("someid1", new StatisticsConsumer() {
      public long getMaximumConsumedDataCount() {
        return -1;
      }

      public boolean consumeStatisticData(StatisticData data) {
        count[0]++;
        return true;
      }
    });
    assertEquals(0, count[0]);
  }

  public void testStoreStatistics() throws Exception {
    buffer.createCaptureSession("someid1");

    final int[] count = new int[] { 0 };

    buffer.storeStatistic(new StatisticData().sessionId("someid1").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("yummy").moment(new Date()).name("the stat").data("stuff"));
    count[0] = 0;
    buffer.consumeStatistics("someid1", new StatisticsConsumer() {
      public long getMaximumConsumedDataCount() {
        return -1;
      }

      public boolean consumeStatisticData(StatisticData data) {
        count[0]++;
        return true;
      }
    });
    assertEquals(1, count[0]);

    buffer.storeStatistic(new StatisticData().sessionId("someid1").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("yummy").moment(new Date()).name("the stat").data("stuff2"));
    count[0] = 0;
    buffer.consumeStatistics("someid1", new StatisticsConsumer() {
      public long getMaximumConsumedDataCount() {
        return -1;
      }

      public boolean consumeStatisticData(StatisticData data) {
        count[0]++;
        return true;
      }
    });
    assertEquals(1, count[0]);

    buffer.createCaptureSession("someid2");

    buffer.storeStatistic(new StatisticData().sessionId("someid2").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("yummy").moment(new Date()).name("the stat 2").data("stuff3"));
    count[0] = 0;
    buffer.consumeStatistics("someid2", new StatisticsConsumer() {
      public long getMaximumConsumedDataCount() {
        return -1;
      }

      public boolean consumeStatisticData(StatisticData data) {
        count[0]++;
        return true;
      }
    });
    assertEquals(1, count[0]);
  }

  public void testConsumeStatisticsInvalidSessionId() throws Exception {
    try {
      buffer.consumeStatistics(null, null);
      fail("expected exception");
    } catch (NullPointerException e) {
      // session ID can't be null
    }
  }

  public void testConsumeStatisticsNullConsumer() throws Exception {
    try {
      buffer.consumeStatistics("someid", null);
      fail("expected exception");
    } catch (NullPointerException e) {
      // consumer can't be null
    }
  }

  public void testConsumeStatisticsUnopenedBuffer() throws Exception {
    buffer.createCaptureSession("someid1");

    buffer.close();
    try {
      buffer.consumeStatistics("someid1", new TestStaticticConsumer());
      fail("expected exception");
    } catch (StatisticsBufferException e) {
      // expected
      assertTrue(e.getCause() instanceof StatisticsDatabaseNotReadyException);
    }
  }

  public void testConsumeStatistics() throws Exception {
    buffer.createCaptureSession("sessionid1");
    buffer.createCaptureSession("sessionid2");
    populateBufferWithStatistics("sessionid1", "sessionid2");

    TestStaticticConsumer consumer1 = new TestStaticticConsumer();
    buffer.consumeStatistics("sessionid1", consumer1);
    consumer1.ensureCorrectCounts(100, 50);

    TestStaticticConsumer consumer2 = new TestStaticticConsumer();
    buffer.consumeStatistics("sessionid1", consumer2);
    consumer2.ensureCorrectCounts(0, 0);

    TestStaticticConsumer consumer3 = new TestStaticticConsumer();
    buffer.consumeStatistics("sessionid2", consumer3);
    consumer3.ensureCorrectCounts(70, 0);

    TestStaticticConsumer consumer4 = new TestStaticticConsumer();
    buffer.consumeStatistics("sessionid2", consumer4);
    consumer4.ensureCorrectCounts(0, 0);
  }

  public void testConsumeStatisticsLimit() throws Exception {
    buffer.createCaptureSession("sessionid1");
    buffer.createCaptureSession("sessionid2");
    populateBufferWithStatistics("sessionid1", "sessionid2");

    final int[] count1 = new int[] { 0 };
    buffer.consumeStatistics("sessionid1", new StatisticsConsumer() {
      public long getMaximumConsumedDataCount() {
        return 20;
      }

      public boolean consumeStatisticData(StatisticData data) {
        count1[0]++;
        return true;
      }
    });

    assertEquals(20, count1[0]);

    final int[] count2 = new int[] { 0 };
    buffer.consumeStatistics("sessionid1", new StatisticsConsumer() {
      public long getMaximumConsumedDataCount() {
        return 2000;
      }

      public boolean consumeStatisticData(StatisticData data) {
        count2[0]++;
        return true;
      }
    });

    assertEquals(130, count2[0]);
  }

  public void testConsumeStatisticsInterruptions() throws Exception {
    buffer.createCaptureSession("sessionid1");
    buffer.createCaptureSession("sessionid2");
    populateBufferWithStatistics("sessionid1", "sessionid2");

    TestStaticticConsumer consumer1 = new TestStaticticConsumer().countLimit1(1);
    buffer.consumeStatistics("sessionid1", consumer1);
    consumer1.ensureCorrectCounts(1, 0);

    TestStaticticConsumer consumer2 = new TestStaticticConsumer().countOffset1(1).countLimit1(98);
    buffer.consumeStatistics("sessionid1", consumer2);
    consumer2.ensureCorrectCounts(98, 0);

    TestStaticticConsumer consumer3 = new TestStaticticConsumer().countOffset1(99).countLimit2(20);
    buffer.consumeStatistics("sessionid1", consumer3);
    consumer3.ensureCorrectCounts(1, 20);

    TestStaticticConsumer consumer4 = new TestStaticticConsumer().countOffset1(100).countOffset2(20);
    buffer.consumeStatistics("sessionid1", consumer4);
    consumer4.ensureCorrectCounts(0, 30);
  }

  public void testConsumeStatisticsExceptions() throws Exception {
    buffer.createCaptureSession("sessionid1");
    buffer.createCaptureSession("sessionid2");
    populateBufferWithStatistics("sessionid1", "sessionid2");

    TestStaticticConsumer consumer1 = new TestStaticticConsumer().countLimit1(1).limitWithExceptions(true);
    try {
      buffer.consumeStatistics("sessionid1", consumer1);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertEquals("stat1 limited", e.getMessage());
    }

    TestStaticticConsumer consumer2 = new TestStaticticConsumer().countOffset1(1).countLimit1(98)
        .limitWithExceptions(true);
    try {
      buffer.consumeStatistics("sessionid1", consumer2);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertEquals("stat1 limited", e.getMessage());
    }
    consumer2.ensureCorrectCounts(98, 0);

    TestStaticticConsumer consumer3 = new TestStaticticConsumer().countOffset1(99).countLimit2(20)
        .limitWithExceptions(true);
    try {
      buffer.consumeStatistics("sessionid1", consumer3);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertEquals("stat2 limited", e.getMessage());
    }
    consumer3.ensureCorrectCounts(1, 20);

    TestStaticticConsumer consumer4 = new TestStaticticConsumer().countOffset1(100).countOffset2(20)
        .limitWithExceptions(true);
    buffer.consumeStatistics("sessionid1", consumer4);
    consumer4.ensureCorrectCounts(0, 30);
  }

  public void testDataTypes() throws Exception {
    buffer.createCaptureSession("sessionid1");
    buffer.createCaptureSession("sessionid2");
    buffer.createCaptureSession("sessionid3");
    buffer.createCaptureSession("sessionid4");

    buffer.storeStatistic(new StatisticData().sessionId("sessionid1")
        .agentIp(InetAddress.getLocalHost().getHostAddress()).agentDifferentiator("yummy").moment(new Date())
        .name("the stat").data("string"));

    final Date date_data = new Date();
    buffer.storeStatistic(new StatisticData().sessionId("sessionid2")
        .agentIp(InetAddress.getLocalHost().getHostAddress()).agentDifferentiator("yummy").moment(new Date())
        .name("the stat").data(date_data));

    buffer.storeStatistic(new StatisticData().sessionId("sessionid3")
        .agentIp(InetAddress.getLocalHost().getHostAddress()).agentDifferentiator("yummy").moment(new Date())
        .name("the stat").data(new Long(28756L)));

    buffer.storeStatistic(new StatisticData().sessionId("sessionid4")
        .agentIp(InetAddress.getLocalHost().getHostAddress()).agentDifferentiator("yummy").moment(new Date())
        .name("the stat").data(new BigDecimal("6828.577")));

    buffer.consumeStatistics("sessionid1", new StatisticsConsumer() {
      public long getMaximumConsumedDataCount() {
        return -1;
      }

      public boolean consumeStatisticData(StatisticData data) {
        assertTrue(data.getData() instanceof String);
        assertEquals("string", data.getData());
        return true;
      }
    });

    buffer.consumeStatistics("sessionid2", new StatisticsConsumer() {
      public long getMaximumConsumedDataCount() {
        return -1;
      }

      public boolean consumeStatisticData(StatisticData data) {
        assertTrue(data.getData() instanceof Date);
        assertEquals(date_data, data.getData());
        return true;
      }
    });

    buffer.consumeStatistics("sessionid3", new StatisticsConsumer() {
      public long getMaximumConsumedDataCount() {
        return -1;
      }

      public boolean consumeStatisticData(StatisticData data) {
        assertTrue(data.getData() instanceof Long);
        assertEquals(new Long(28756L), data.getData());
        return true;
      }
    });

    buffer.consumeStatistics("sessionid4", new StatisticsConsumer() {
      public long getMaximumConsumedDataCount() {
        return -1;
      }

      public boolean consumeStatisticData(StatisticData data) {
        assertTrue(data.getData() instanceof BigDecimal);
        assertEquals(0, new BigDecimal("6828.577").compareTo((BigDecimal) data.getData()));
        return true;
      }
    });
  }

  public void testStatisticsBufferListeners() throws Exception {
    buffer.createCaptureSession("someid1");
    TestStatisticsBufferListener listener1 = new TestStatisticsBufferListener("someid1");
    buffer.addListener(listener1);
    TestStatisticsBufferListener listener2 = new TestStatisticsBufferListener("someid1");
    buffer.addListener(listener2);

    assertFalse(listener1.isStarted());
    assertFalse(listener1.isStopped());

    buffer.startCapturing("someid1");

    assertTrue(listener1.isStarted());
    assertFalse(listener1.isStopped());

    buffer.stopCapturing("someid1");

    assertTrue(listener1.isStarted());
    assertTrue(listener1.isStopped());

    assertFalse(listener1.isClosing());
    assertFalse(listener2.isClosing());
    assertFalse(listener1.isClosed());
    assertFalse(listener2.isClosed());

    buffer.close();

    assertTrue(listener1.isClosing());
    assertTrue(listener2.isClosing());
    assertTrue(listener1.isClosed());
    assertTrue(listener2.isClosed());

    assertFalse(listener1.isOpened());
    assertFalse(listener2.isOpened());

    buffer.open();

    assertTrue(listener1.isOpened());
    assertTrue(listener2.isOpened());
  }

  public void testStartCapturingException() throws Exception {
    buffer.createCaptureSession("sessionid");
    buffer.startCapturing("sessionid");
    buffer.startCapturing("sessionid");
    buffer.stopCapturing("sessionid");
    try {
      buffer.startCapturing("sessionid");
      fail();
    } catch (StatisticsBufferException e) {
      // expected
    }
  }

  public void testStopCapturingPermissive() throws Exception {
    buffer.createCaptureSession("thissessionid1");
    buffer.stopCapturing("thissessionid1");
    buffer.stopCapturing("thissessionid1");

    buffer.createCaptureSession("thissessionid2");
    buffer.startCapturing("thissessionid2");
    buffer.stopCapturing("thissessionid2");
    buffer.stopCapturing("thissessionid2");

    try {
      buffer.stopCapturing("thissessionid3");
      fail();
    } catch (StatisticsBufferException e) {
      // expected
    }
  }

  private void populateBufferWithStatistics(String sessionid1, String sessionid2) throws StatisticsBufferException,
      UnknownHostException {
    String ip = InetAddress.getLocalHost().getHostAddress();
    for (int i = 1; i <= 100; i++) {
      buffer.storeStatistic(new StatisticData().sessionId(sessionid1).agentIp(ip).agentDifferentiator("D1")
          .moment(new Date()).name("stat1").data(new Long(i)));
    }
    for (int i = 1; i <= 50; i++) {
      buffer.storeStatistic(new StatisticData().sessionId(sessionid1).agentIp(ip).agentDifferentiator("D2")
          .moment(new Date()).name("stat2").data(String.valueOf(i)));
    }

    for (int i = 1; i <= 70; i++) {
      buffer.storeStatistic(new StatisticData().sessionId(sessionid2).agentIp(ip).agentDifferentiator("D3")
          .moment(new Date()).name("stat1").data(new BigDecimal(String.valueOf(i + ".0"))));
    }
  }

  private class TestStaticticConsumer implements StatisticsConsumer {
    private int     statCount1          = 0;
    private int     statCount2          = 0;

    private int     countOffset1        = 0;
    private int     countOffset2        = 0;

    private int     countLimit1         = 0;
    private int     countLimit2         = 0;

    private boolean limitWithExceptions = false;

    public TestStaticticConsumer countOffset1(int countOffset) {
      this.countOffset1 = countOffset;
      return this;
    }

    public TestStaticticConsumer countOffset2(int countOffset) {
      this.countOffset2 = countOffset;
      return this;
    }

    public TestStaticticConsumer countLimit1(int countLimit) {
      this.countLimit1 = countLimit;
      return this;
    }

    public TestStaticticConsumer countLimit2(int countLimit) {
      this.countLimit2 = countLimit;
      return this;
    }

    public TestStaticticConsumer limitWithExceptions(boolean limitWithExceptionsArg) {
      this.limitWithExceptions = limitWithExceptionsArg;
      return this;
    }

    public long getMaximumConsumedDataCount() {
      return -1;
    }

    public boolean consumeStatisticData(StatisticData data) {
      if (data.getName().equals("stat1")) {
        if (countLimit1 > 0 && countLimit1 == statCount1) {
          if (limitWithExceptions) {
            throw new RuntimeException("stat1 limited");
          } else {
            return false;
          }
        }
        statCount1++;
        if (data.getData() instanceof BigDecimal) {
          assertEquals("D3", data.getAgentDifferentiator());
        } else {
          assertEquals("D1", data.getAgentDifferentiator());
        }
        assertEquals(((Number) data.getData()).longValue(), statCount1 + countOffset1);
      }
      if (data.getName().equals("stat2")) {
        if (countLimit2 > 0 && countLimit2 == statCount2) {
          if (limitWithExceptions) {
            throw new RuntimeException("stat2 limited");
          } else {
            return false;
          }
        }
        statCount2++;
        assertEquals("D2", data.getAgentDifferentiator());
        assertEquals(String.valueOf(data.getData()), String.valueOf(statCount2 + countOffset2));
      }
      return true;
    }

    public void ensureCorrectCounts(int count1, int count2) {
      assertEquals(count1, statCount1);
      assertEquals(count2, statCount2);
    }
  }

  private class TestStatisticsBufferListener implements StatisticsBufferListener {
    private final String sessionId;
    private boolean      started = false;
    private boolean      stopped = false;
    private boolean      opened  = false;
    private boolean      closing = false;
    private boolean      closed  = false;

    public TestStatisticsBufferListener(String sessionId) {
      this.sessionId = sessionId;
    }

    public boolean isStarted() {
      return started;
    }

    public boolean isStopped() {
      return stopped;
    }

    public boolean isOpened() {
      return opened;
    }

    public boolean isClosing() {
      return closing;
    }

    public boolean isClosed() {
      return closed;
    }

    public void capturingStarted(String sessionID) {
      assertEquals(false, started);
      assertEquals(false, stopped);
      assertEquals(this.sessionId, sessionID);
      started = true;
    }

    public void capturingStopped(String sessionID) {
      assertEquals(true, started);
      assertEquals(false, stopped);
      assertEquals(this.sessionId, sessionID);
      stopped = true;
    }

    public void opened() {
      opened = true;
    }

    public void closing() {
      closing = true;
    }

    public void closed() {
      closed = true;
    }
  }
}