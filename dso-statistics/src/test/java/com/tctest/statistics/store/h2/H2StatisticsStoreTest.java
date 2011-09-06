/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.statistics.store.h2;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.InputSource;

import com.tc.statistics.StatisticData;
import com.tc.statistics.database.exceptions.StatisticsDatabaseNotReadyException;
import com.tc.statistics.database.exceptions.StatisticsDatabaseStructureFuturedatedError;
import com.tc.statistics.database.exceptions.StatisticsDatabaseStructureOutdatedError;
import com.tc.statistics.database.impl.H2StatisticsDatabase;
import com.tc.statistics.jdbc.JdbcHelper;
import com.tc.statistics.store.StatisticDataUser;
import com.tc.statistics.store.StatisticsRetrievalCriteria;
import com.tc.statistics.store.StatisticsStore;
import com.tc.statistics.store.StatisticsStoreImportListener;
import com.tc.statistics.store.StatisticsStoreListener;
import com.tc.statistics.store.TextualDataFormat;
import com.tc.statistics.store.exceptions.StatisticsStoreException;
import com.tc.statistics.store.h2.H2StatisticsStoreImpl;
import com.tc.test.TempDirectoryHelper;
import com.tc.util.TCAssertionError;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

public class H2StatisticsStoreTest extends TestCase {
  private StatisticsStore store;
  private File            tmpDir;

  private final Random    random = new Random();

  @Override
  public void setUp() throws Exception {
    tmpDir = createTmpDir();
    store = new H2StatisticsStoreImpl(tmpDir);
    store.open();
  }

  private File createTmpDir() throws IOException {
    File tmp_dir_parent = new TempDirectoryHelper(getClass(), false).getDirectory();
    File tmp_dir;
    synchronized (random) {
      tmp_dir = new File(tmp_dir_parent, "statisticsstore-" + random.nextInt() + "-" + System.currentTimeMillis());
    }
    tmp_dir.mkdirs();
    return tmp_dir;
  }

  @Override
  public void tearDown() throws Exception {
    store.close();
  }

  public void testInvalidBufferDirectory() throws Exception {
    try {
      new H2StatisticsStoreImpl(null);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // dir can't be null
    }

    File tmp_dir = createTmpDir();
    tmp_dir.delete();
    try {
      new H2StatisticsStoreImpl(tmp_dir);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // dir doesn't exist
    }

    tmp_dir = createTmpDir();
    tmp_dir.delete();
    tmp_dir.createNewFile();
    try {
      new H2StatisticsStoreImpl(tmp_dir);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // path is not a dir
    } finally {
      tmp_dir.delete();
    }

    tmp_dir = createTmpDir();
    boolean tmpDirReadOnly = tmp_dir.setReadOnly();

    if (tmpDirReadOnly) {

      if (tmp_dir.canWrite()) {
        System.err.println(tmp_dir + " is set read only but still writable. Probably an OS issue ?");
        return;
      }

      try {
        new H2StatisticsStoreImpl(tmp_dir);
        fail("unexpected exception");
      } catch (TCAssertionError e) {
        // dir is not writable
      } finally {
        tmp_dir.delete();
      }
    } else {
      System.err.println(tmp_dir.getAbsolutePath() + " could not be set read only.");
    }
  }

  public void testOutdatedVersionCheck() throws Exception {
    store.close();

    H2StatisticsDatabase database = new H2StatisticsDatabase(tmpDir, H2StatisticsStoreImpl.H2_URL_SUFFIX);
    database.open();
    try {
      JdbcHelper.executeUpdate(database.getConnection(), "UPDATE dbstructureversion SET version = "
                                                         + (H2StatisticsStoreImpl.DATABASE_STRUCTURE_VERSION - 1));
      try {
        store.open();
        fail("expected exception");
      } catch (StatisticsDatabaseStructureOutdatedError e) {
        assertEquals(H2StatisticsStoreImpl.DATABASE_STRUCTURE_VERSION - 1, e.getActualVersion());
        assertEquals(H2StatisticsStoreImpl.DATABASE_STRUCTURE_VERSION, e.getExpectedVersion());
        assertNotNull(e.getCreationDate());
      }
    } finally {
      database.close();
    }
  }

  public void testFuturedatedVersionCheck() throws Exception {
    store.close();

    H2StatisticsDatabase database = new H2StatisticsDatabase(tmpDir, H2StatisticsStoreImpl.H2_URL_SUFFIX);
    database.open();
    try {
      JdbcHelper.executeUpdate(database.getConnection(), "UPDATE dbstructureversion SET version = "
                                                         + (H2StatisticsStoreImpl.DATABASE_STRUCTURE_VERSION + 1));
      try {
        store.open();
        fail("expected exception");
      } catch (StatisticsDatabaseStructureFuturedatedError e) {
        assertEquals(H2StatisticsStoreImpl.DATABASE_STRUCTURE_VERSION + 1, e.getActualVersion());
        assertEquals(H2StatisticsStoreImpl.DATABASE_STRUCTURE_VERSION, e.getExpectedVersion());
        assertNotNull(e.getCreationDate());
      }
    } finally {
      database.close();
    }
  }

  public void testOpenClose() throws Exception {
    TestStatisticsStoreListener listener = new TestStatisticsStoreListener();
    store.addListener(listener);

    // several opens and closes are silently detected
    assertFalse(listener.opened);
    store.open();
    assertTrue(listener.opened);
    listener.reset();
    assertFalse(listener.opened);
    store.open();
    assertTrue(listener.opened);
    assertFalse(listener.closed);
    store.close();
    assertTrue(listener.closed);
    listener.reset();
    assertFalse(listener.closed);
    store.close();
    assertTrue(listener.closed);
  }

  public void testCloseUnopenedBuffer() throws Exception {
    store.close();

    File tmp_dir = createTmpDir();
    StatisticsStore newStore = new H2StatisticsStoreImpl(tmp_dir);
    newStore.close(); // should not throw an exception
  }

  public void testStoreStatisticsDataNullSessionId() throws Exception {
    try {
      store.storeStatistic(new StatisticData().agentIp(InetAddress.getLocalHost().getHostAddress())
          .agentDifferentiator("L1/0").moment(new Date()).name("name"));
      fail("expected exception");
    } catch (NullPointerException e) {
      // sessionId can't be null
    }
  }

  public void testStoreStatisticsDataNullAgentIp() throws Exception {
    try {
      store.storeStatistic(new StatisticData().sessionId("374938").agentDifferentiator("L1/0").moment(new Date())
          .name("name"));
      fail("expected exception");
    } catch (NullPointerException e) {
      // agentIp can't be null
    }
  }

  public void testStoreStatisticsDataNullAgentDifferentiator() throws Exception {
    try {
      store.storeStatistic(new StatisticData().sessionId("374938").agentIp(InetAddress.getLocalHost().getHostAddress())
          .moment(new Date()).name("name"));
      fail("expected exception");
    } catch (NullPointerException e) {
      // agentDifferentiator can't be null
    }
  }

  public void testStoreStatisticsDataNullMoment() throws Exception {
    try {
      store.storeStatistic(new StatisticData().sessionId("374938").agentIp(InetAddress.getLocalHost().getHostAddress())
          .agentDifferentiator("L1/0").name("name"));
      fail("expected exception");
    } catch (NullPointerException e) {
      // moment can't be null
    }
  }

  public void testStoreStatisticsDataNullName() throws Exception {
    try {
      store.storeStatistic(new StatisticData().sessionId("374938").agentIp(InetAddress.getLocalHost().getHostAddress())
          .agentDifferentiator("L1/0").moment(new Date()));
      fail("expected exception");
    } catch (NullPointerException e) {
      // name can't be null
    }
  }

  public void testStoreStatisticsDataNullData() throws Exception {
    store.storeStatistic(new StatisticData().sessionId("342").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("L1/0").moment(new Date()).name("name"));
  }

  public void testStoreStatisticsUnopenedBuffer() throws Exception {
    store.close();
    try {
      store.storeStatistic(new StatisticData().sessionId("342").agentIp(InetAddress.getLocalHost().getHostAddress())
          .agentDifferentiator("L1/0").moment(new Date()).name("name").data("test"));
      fail("expected exception");
    } catch (StatisticsStoreException e) {
      // expected
      assertTrue(e.getCause() instanceof StatisticsDatabaseNotReadyException);
    }
  }

  public void testReinitialize() throws Exception {
    store.storeStatistic(new StatisticData().sessionId("376487").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("L1/0").moment(new Date()).name("the stat").data("stuff"));

    store.reinitialize();

    final int[] count = new int[] { 0 };
    store.retrieveStatistics(new StatisticsRetrievalCriteria(), new StatisticDataUser() {
      public boolean useStatisticData(final StatisticData data) {
        count[0]++;
        return true;
      }
    });
    assertEquals(0, count[0]);
  }

  public void testStoreStatistics() throws Exception {
    final int[] count = new int[] { 0 };
    store.storeStatistic(new StatisticData().sessionId("376487").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("L1/0").moment(new Date()).name("the stat").data("stuff"));
    count[0] = 0;
    store.retrieveStatistics(new StatisticsRetrievalCriteria(), new StatisticDataUser() {
      public boolean useStatisticData(final StatisticData data) {
        count[0]++;
        return true;
      }
    });
    assertEquals(1, count[0]);

    store.storeStatistic(new StatisticData().sessionId("376487").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("L1/0").moment(new Date()).name("the stat").data("stuff2"));
    count[0] = 0;
    store.retrieveStatistics(new StatisticsRetrievalCriteria(), new StatisticDataUser() {
      public boolean useStatisticData(final StatisticData data) {
        count[0]++;
        return true;
      }
    });
    assertEquals(2, count[0]);

    store.storeStatistic(new StatisticData().sessionId("2232").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("L1/0").moment(new Date()).name("the stat 2").data("stuff3"));
    count[0] = 0;
    store.retrieveStatistics(new StatisticsRetrievalCriteria(), new StatisticDataUser() {
      public boolean useStatisticData(final StatisticData data) {
        count[0]++;
        return true;
      }
    });
    assertEquals(3, count[0]);
  }

  public void testRetrieveStatistics() throws Exception {
    String sessionid1 = "34987";
    String sessionid2 = "9367";

    Date before = new Date();
    Thread.sleep(500);
    populateBufferWithStatistics(sessionid1, sessionid2);
    Thread.sleep(500);
    Date after = new Date();

    TestStaticticConsumer consumer1 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria(), consumer1);
    consumer1.ensureCorrectCounts(170, 50);

    TestStaticticConsumer consumer2 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1).addName("stat1"), consumer2);
    consumer2.ensureCorrectCounts(100, 0);

    TestStaticticConsumer consumer3 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria().agentIp(InetAddress.getLocalHost().getHostAddress())
        .sessionId(sessionid1).addName("stat1").addName("stat2"), consumer3);
    consumer3.ensureCorrectCounts(100, 50);

    TestStaticticConsumer consumer4 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria().agentIp("unknown").sessionId(sessionid2), consumer4);
    consumer4.ensureCorrectCounts(0, 0);

    TestStaticticConsumer consumer5 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid2).addName("stat1")
                                 .addElement("element1"), consumer5);
    consumer5.ensureCorrectCounts(70, 0);

    TestStaticticConsumer consumer6 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1).addName("stat1")
                                 .addElement("element1"), consumer6);
    consumer6.ensureCorrectCounts(100, 0);

    TestStaticticConsumer consumer7 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1).addElement("element1")
                                 .addElement("element2"), consumer7);
    consumer7.ensureCorrectCounts(100, 50);

    TestStaticticConsumer consumer8 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria().start(before), consumer8);
    consumer8.ensureCorrectCounts(170, 50);

    TestStaticticConsumer consumer9 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria().stop(before), consumer9);
    consumer9.ensureCorrectCounts(0, 0);

    TestStaticticConsumer consumer10 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria().start(after), consumer10);
    consumer10.ensureCorrectCounts(0, 0);

    TestStaticticConsumer consumer11 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria().start(before).stop(after), consumer11);
    consumer11.ensureCorrectCounts(170, 50);

    TestStaticticConsumer consumer12 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria().agentDifferentiator("D2").addName("stat1"), consumer12);
    consumer12.ensureCorrectCounts(70, 0);

    TestStaticticConsumer consumer13 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria().agentDifferentiator("D1").addName("stat1"), consumer13);
    consumer13.ensureCorrectCounts(100, 0);
  }

  public void testRetrieveStatisticsInterruptions() throws Exception {
    String sessionid1 = "34987";
    String sessionid2 = "9367";
    populateBufferWithStatistics(sessionid1, sessionid2);

    TestStaticticConsumer consumer1 = new TestStaticticConsumer().countLimit1(1);
    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1), consumer1);
    consumer1.ensureCorrectCounts(1, 0);

    TestStaticticConsumer consumer2 = new TestStaticticConsumer().countLimit1(98);
    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1), consumer2);
    consumer2.ensureCorrectCounts(98, 0);

    TestStaticticConsumer consumer3 = new TestStaticticConsumer().countLimit2(20);
    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1), consumer3);
    consumer3.ensureCorrectCounts(100, 20);

    TestStaticticConsumer consumer4 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1), consumer4);
    consumer4.ensureCorrectCounts(100, 50);
  }

  public void testRetrieveStatisticsExceptions() throws Exception {
    String sessionid1 = "34987";
    String sessionid2 = "9367";
    populateBufferWithStatistics(sessionid1, sessionid2);

    TestStaticticConsumer consumer1 = new TestStaticticConsumer().countLimit1(1).limitWithExceptions(true);
    try {
      store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1), consumer1);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertEquals("stat1 limited", e.getMessage());
    }
    consumer1.ensureCorrectCounts(1, 0);

    TestStaticticConsumer consumer2 = new TestStaticticConsumer().countLimit1(98).limitWithExceptions(true);
    try {
      store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1), consumer2);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertEquals("stat1 limited", e.getMessage());
    }
    consumer2.ensureCorrectCounts(98, 0);

    TestStaticticConsumer consumer3 = new TestStaticticConsumer().countLimit2(20).limitWithExceptions(true);
    try {
      store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1), consumer3);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertEquals("stat2 limited", e.getMessage());
    }
    consumer3.ensureCorrectCounts(100, 20);

    TestStaticticConsumer consumer4 = new TestStaticticConsumer().limitWithExceptions(true);
    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1), consumer4);
    consumer4.ensureCorrectCounts(100, 50);
  }

  public void testDataTypes() throws Exception {
    store.storeStatistic(new StatisticData().sessionId("sessionid1")
        .agentIp(InetAddress.getLocalHost().getHostAddress()).agentDifferentiator("yummy").moment(new Date())
        .name("the stat").data("string"));

    final Date date_data = new Date();
    store.storeStatistic(new StatisticData().sessionId("sessionid2")
        .agentIp(InetAddress.getLocalHost().getHostAddress()).agentDifferentiator("yummy").moment(new Date())
        .name("the stat").data(date_data));

    store.storeStatistic(new StatisticData().sessionId("sessionid3")
        .agentIp(InetAddress.getLocalHost().getHostAddress()).agentDifferentiator("yummy").moment(new Date())
        .name("the stat").data(new Long(28756L)));

    store.storeStatistic(new StatisticData().sessionId("sessionid4")
        .agentIp(InetAddress.getLocalHost().getHostAddress()).agentDifferentiator("yummy").moment(new Date())
        .name("the stat").data(new BigDecimal("6828.577")));

    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId("sessionid1"), new StatisticDataUser() {
      public boolean useStatisticData(final StatisticData data) {
        assertTrue(data.getData() instanceof String);
        assertEquals("string", data.getData());
        return true;
      }
    });

    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId("sessionid2"), new StatisticDataUser() {
      public boolean useStatisticData(final StatisticData data) {
        assertTrue(data.getData() instanceof Date);
        assertEquals(date_data, data.getData());
        return true;
      }
    });

    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId("sessionid3"), new StatisticDataUser() {
      public boolean useStatisticData(final StatisticData data) {
        assertTrue(data.getData() instanceof Long);
        assertEquals(new Long(28756L), data.getData());
        return true;
      }
    });

    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId("sessionid4"), new StatisticDataUser() {
      public boolean useStatisticData(final StatisticData data) {
        assertTrue(data.getData() instanceof BigDecimal);
        assertEquals(0, new BigDecimal("6828.577").compareTo((BigDecimal) data.getData()));
        return true;
      }
    });
  }

  public void testGetAvailableSessionIds() throws Exception {
    store.storeStatistic(new StatisticData().sessionId("376487").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("L1/0").moment(new Date()).name("the stat").data("stuff"));
    store.storeStatistic(new StatisticData().sessionId("12").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("L1/0").moment(new Date()).name("the stat 2").data("stuff3"));
    store.storeStatistic(new StatisticData().sessionId("376487").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("L1/0").moment(new Date()).name("the stat").data("stuff2"));
    store.storeStatistic(new StatisticData().sessionId("2232").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("L1/0").moment(new Date()).name("the stat 2").data("stuff3"));
    store.storeStatistic(new StatisticData().sessionId("12").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("L1/0").moment(new Date()).name("the stat 2").data("stuff3"));

    String[] sessionids = store.getAvailableSessionIds();
    assertEquals(3, sessionids.length);
    assertEquals("12", sessionids[0]);
    assertEquals("2232", sessionids[1]);
    assertEquals("376487", sessionids[2]);
  }

  public void testGetAvailableAgentDifferentiators() throws Exception {
    store.storeStatistic(new StatisticData().sessionId("376487").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("L1/0").moment(new Date()).name("the stat").data("stuff"));
    store.storeStatistic(new StatisticData().sessionId("12").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("L1/2").moment(new Date()).name("the stat 2").data("stuff3"));
    store.storeStatistic(new StatisticData().sessionId("376487").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("L1/0").moment(new Date()).name("the stat").data("stuff2"));
    store.storeStatistic(new StatisticData().sessionId("2232").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("L1/1").moment(new Date()).name("the stat 2").data("stuff3"));
    store.storeStatistic(new StatisticData().sessionId("12").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("L1/1").moment(new Date()).name("the stat 2").data("stuff3"));

    String[] agentdifferentiators1 = store.getAvailableAgentDifferentiators("376487");
    assertEquals(1, agentdifferentiators1.length);
    assertEquals("L1/0", agentdifferentiators1[0]);

    String[] agentdifferentiators2 = store.getAvailableAgentDifferentiators("2232");
    assertEquals(1, agentdifferentiators2.length);
    assertEquals("L1/1", agentdifferentiators2[0]);

    String[] agentdifferentiators3 = store.getAvailableAgentDifferentiators("12");
    assertEquals(2, agentdifferentiators3.length);
    assertEquals("L1/1", agentdifferentiators3[0]);
    assertEquals("L1/2", agentdifferentiators3[1]);
  }

  public void testClearStatistics() throws Exception {
    TestStatisticsStoreListener listener = new TestStatisticsStoreListener();
    store.addListener(listener);

    String sessionid1 = "34987";
    String sessionid2 = "9367";

    Thread.sleep(500);
    populateBufferWithStatistics(sessionid1, sessionid2);
    Thread.sleep(500);

    TestStaticticConsumer consumer1 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria(), consumer1);
    consumer1.ensureCorrectCounts(170, 50);

    assertNull(listener.sessionCleared);
    store.clearStatistics(sessionid2);
    assertEquals(sessionid2, listener.sessionCleared);

    TestStaticticConsumer consumer2 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria(), consumer2);
    consumer2.ensureCorrectCounts(100, 50);

    listener.reset();
    assertNull(listener.sessionCleared);
    store.clearStatistics(sessionid1);
    assertEquals(sessionid1, listener.sessionCleared);

    TestStaticticConsumer consumer3 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria(), consumer3);
    consumer3.ensureCorrectCounts(0, 0);
  }

  public void testClearAllStatistics() throws Exception {
    TestStatisticsStoreListener listener = new TestStatisticsStoreListener();
    store.addListener(listener);

    String sessionid1 = "34987";
    String sessionid2 = "9367";

    Thread.sleep(500);
    populateBufferWithStatistics(sessionid1, sessionid2);
    Thread.sleep(500);

    TestStaticticConsumer consumer1 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria(), consumer1);
    consumer1.ensureCorrectCounts(170, 50);

    assertNull(listener.sessionCleared);
    store.clearAllStatistics();
    assertTrue(listener.allSessionsCleared);

    TestStaticticConsumer consumer2 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria(), consumer2);
    consumer2.ensureCorrectCounts(0, 0);
  }

  public void testAggregateStatisticsDataCsv() throws Exception {
    populateBufferWithStatistics("somesession1", "somesession2", 10, 8, 4, 6);

    StringWriter writer1 = new StringWriter();
    store.aggregateStatisticsData(writer1, TextualDataFormat.CSV, "somesession1", "D1",
                                  new String[] { "stat1", "stat2" }, null, null);
    String result1 = writer1.getBuffer().toString();
    System.out.println("result1 : " + result1);
    String[] result1b = StringUtils.split(result1, "\n");
    assertEquals(result1b.length, 1);
    assertEquals(",1,2,3,4,5,6,7,8,9,10", result1.substring(result1.indexOf(',')));

    StringWriter writer2 = new StringWriter();
    store.aggregateStatisticsData(writer2, TextualDataFormat.CSV, "somesession2", "D2",
                                  new String[] { "stat1", "stat2" }, null, null);
    String result2 = writer2.getBuffer().toString();
    System.out.println("result2 : " + result2);
    String[] result2b = StringUtils.split(result2, "\n");
    assertEquals(result2b.length, 2);
    assertEquals(",1.0000,2.0000,3.0000,4.0000", result2b[0].substring(result2b[0].indexOf(',')));
    int index_comma = result2b[1].indexOf(',');
    long moment = Long.parseLong(result2b[1].substring(0, index_comma));
    assertEquals(moment + "," + (moment + 1) + "," + (moment + 2) + "," + (moment + 3) + "," + (moment + 4) + ","
                 + (moment + 5) + "," + (moment + 6), result2b[1]);

    StringWriter writer3 = new StringWriter();
    store.aggregateStatisticsData(writer3, TextualDataFormat.CSV, "somesession1", "D1",
                                  new String[] { "stat1", "stat2" }, null, new Long(3000));
    String result3 = writer3.getBuffer().toString();
    System.out.println("result3 : " + result3);
    assertTrue(result3.length() > 0);

    Thread.sleep(4000);

    StringWriter writer4 = new StringWriter();
    store.aggregateStatisticsData(writer4, TextualDataFormat.CSV, "somesession1", "D1",
                                  new String[] { "stat1", "stat2" }, null, new Long(3000));
    String result4 = writer4.getBuffer().toString();
    System.out.println("result4 : " + result4);
    assertTrue(0 == result4.length());
  }

  public void testAggregateStatisticsDataXml() throws Exception {
    populateBufferWithStatistics("somesession1", "somesession2", 10, 8, 4, 6);

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();

    StringWriter writer1 = new StringWriter();
    store.aggregateStatisticsData(writer1, TextualDataFormat.XML, "somesession1", "D1",
                                  new String[] { "stat1", "stat2" }, null, null);
    String result1 = writer1.getBuffer().toString();
    System.out.println("result1 : " + result1);
    String[] result1b = StringUtils.split(result1, "\n");
    assertEquals(result1b.length, 4);
    assertEquals("</m><v1>1</v1><v2>2</v2><v3>3</v3><v4>4</v4><v5>5</v5><v6>6</v6><v7>7</v7><v8>8</v8><v9>9</v9><v10>10</v10></d>",
                 result1b[2].substring(result1b[2].indexOf("</m>")));

    db.parse(new InputSource(new StringReader(result1)));

    StringWriter writer2 = new StringWriter();
    store.aggregateStatisticsData(writer2, TextualDataFormat.XML, "somesession2", "D2",
                                  new String[] { "stat1", "stat2" }, null, null);
    String result2 = writer2.getBuffer().toString();
    System.out.println("result2 : " + result2);
    String[] result2b = StringUtils.split(result2, "\n");
    assertEquals(result2b.length, 5);
    assertEquals("</m><v1>1.0000</v1><v2>2.0000</v2><v3>3.0000</v3><v4>4.0000</v4></d>",
                 result2b[2].substring(result2b[2].indexOf("</m>")));
    int index_start = result2b[3].indexOf("<m>");
    int index_end = result2b[3].indexOf("</m>");
    long moment = Long.parseLong(result2b[3].substring(index_start + 3, index_end));
    assertEquals("<d><m>" + moment + "</m><v1>" + (moment + 1) + "</v1><v2>" + (moment + 2) + "</v2><v3>"
                 + (moment + 3) + "</v3><v4>" + (moment + 4) + "</v4><v5>" + (moment + 5) + "</v5><v6>" + (moment + 6)
                 + "</v6></d>", result2b[3]);

    db.parse(new InputSource(new StringReader(result2)));

    StringWriter writer3 = new StringWriter();
    store.aggregateStatisticsData(writer3, TextualDataFormat.XML, "somesession1", "D1",
                                  new String[] { "stat1", "stat2" }, null, new Long(10000));
    String result3 = writer3.getBuffer().toString();
    System.out.println("result3 : " + result3);
    String[] result3b = StringUtils.split(result3, "\n");
    assertTrue(result3b.length > 3);

    db.parse(new InputSource(new StringReader(result3)));

    Thread.sleep(11000);

    StringWriter writer4 = new StringWriter();
    store.aggregateStatisticsData(writer4, TextualDataFormat.XML, "somesession1", "D1",
                                  new String[] { "stat1", "stat2" }, null, new Long(10000));
    String result4 = writer4.getBuffer().toString();
    System.out.println("result4 : " + result4);
    String[] result4b = StringUtils.split(result4, "\n");
    assertTrue(3 == result4b.length);
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", result4b[0]);
    assertEquals("<data>", result4b[1]);
    assertEquals("</data>", result4b[2]);

    db.parse(new InputSource(new StringReader(result4)));
  }

  public void testCsvImport() throws Exception {
    populateBufferWithStatistics("somesession1", "somesession2", 5000, 4000, 2500, 2000);

    final StringBuffer txt_buffer_before = new StringBuffer();
    final int[] count_before = new int[] { 0 };

    store.retrieveStatistics(new StatisticsRetrievalCriteria(), new StatisticDataUser() {
      public boolean useStatisticData(final StatisticData data) {
        txt_buffer_before.append(data.toString());
        txt_buffer_before.append("\n");
        count_before[0]++;
        return true;
      }
    });

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    store.retrieveStatisticsAsCsvStream(os, "", new StatisticsRetrievalCriteria(), false);
    String csv_buffer = os.toString("UTF-8");

    store.reinitialize();

    store.retrieveStatistics(new StatisticsRetrievalCriteria(), new StatisticDataUser() {
      public boolean useStatisticData(final StatisticData data) {
        fail("The store should be empty.");
        return true;
      }
    });

    final boolean[] started = new boolean[] { false };
    final long[] imported = new long[] { 0 };
    final boolean[] optimizing = new boolean[] { false };
    final long[] finished = new long[] { 0 };
    Reader reader = new StringReader(csv_buffer.toString());
    store.importCsvStatistics(reader, new StatisticsStoreImportListener() {
      public void started() {
        started[0] = true;
      }

      public void imported(final long count) {
        assertTrue(imported[0] < count);
        imported[0] = count;
      }

      public void optimizing() {
        optimizing[0] = true;
      }

      public void finished(final long total) {
        finished[0] = total;
      }
    });

    assertTrue(started[0]);
    assertEquals(13500, imported[0]);
    assertTrue(optimizing[0]);
    assertEquals(13500, finished[0]);

    final StringBuffer txt_buffer_after = new StringBuffer();
    final int[] count_after = new int[] { 0 };
    store.retrieveStatistics(new StatisticsRetrievalCriteria(), new StatisticDataUser() {
      public boolean useStatisticData(final StatisticData data) {
        txt_buffer_after.append(data.toString());
        txt_buffer_after.append("\n");
        count_after[0]++;
        return true;
      }
    });

    assertEquals(txt_buffer_before.toString(), txt_buffer_after.toString());
    assertEquals(count_before[0], count_after[0]);
  }

  private void populateBufferWithStatistics(final String sessionid1, final String sessionid2)
      throws StatisticsStoreException, UnknownHostException, InterruptedException {
    populateBufferWithStatistics(sessionid1, sessionid2, 100, 50, 70, 0);
  }

  private void populateBufferWithStatistics(final String sessionid1, final String sessionid2,
                                            final int sess1stat1count, final int sess1stat2count,
                                            final int sess2stat1count, final int sess2stat2count)
      throws StatisticsStoreException, UnknownHostException, InterruptedException {
    String ip = InetAddress.getLocalHost().getHostAddress();
    Date date1 = new Date();
    for (int i = 1; i <= sess1stat1count; i++) {
      store.storeStatistic(new StatisticData().sessionId(sessionid1).agentIp(ip).agentDifferentiator("D1")
          .moment(date1).name("stat1").element("element1").data(new Long(i)));
    }
    Thread.sleep(100);
    Date date2 = new Date();
    for (int i = 1; i <= sess1stat2count; i++) {
      store.storeStatistic(new StatisticData().sessionId(sessionid1).agentIp(ip).agentDifferentiator("D2")
          .moment(date2).name("stat2").element("element2").data(String.valueOf(i)));
    }
    Thread.sleep(100);
    Date date3 = new Date();
    for (int i = 1; i <= sess2stat1count; i++) {
      store.storeStatistic(new StatisticData().sessionId(sessionid2).agentIp(ip).agentDifferentiator("D2")
          .moment(date3).name("stat1").element("element1").data(new BigDecimal(String.valueOf(i + ".0"))));
    }
    Thread.sleep(100);
    Date date4 = new Date();
    for (int i = 1; i <= sess2stat2count; i++) {
      store.storeStatistic(new StatisticData().sessionId(sessionid2).agentIp(ip).agentDifferentiator("D2")
          .moment(date4).name("stat2").element("element1").data(new Date(date4.getTime() + i)));
    }
    Thread.sleep(100);
  }

  private class TestStaticticConsumer implements StatisticDataUser {
    private int       statCount1          = 0;
    private int       statCount2          = 0;

    private int       countLimit1         = 0;
    private int       countLimit2         = 0;

    private final Map lastDataPerSession  = new HashMap();

    private boolean   limitWithExceptions = false;

    public TestStaticticConsumer countLimit1(final int countLimit) {
      this.countLimit1 = countLimit;
      return this;
    }

    public TestStaticticConsumer countLimit2(final int countLimit) {
      this.countLimit2 = countLimit;
      return this;
    }

    public TestStaticticConsumer limitWithExceptions(final boolean limitWithExceptionsArg) {
      this.limitWithExceptions = limitWithExceptionsArg;
      return this;
    }

    public boolean useStatisticData(final StatisticData data) {
      StatisticData previous = (StatisticData) lastDataPerSession.get(data.getSessionId());
      if (previous != null) {
        assertTrue(previous.getMoment().compareTo(data.getMoment()) <= 0);
      }

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
          assertEquals("D2", data.getAgentDifferentiator());
        } else {
          assertEquals("D1", data.getAgentDifferentiator());
        }
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
      }

      lastDataPerSession.put(data.getSessionId(), data);

      return true;
    }

    public void ensureCorrectCounts(final int count1, final int count2) {
      assertEquals(count1, statCount1);
      assertEquals(count2, statCount2);
    }
  }

  private static class TestStatisticsStoreListener implements StatisticsStoreListener {
    private boolean opened             = false;
    private boolean closed             = false;
    private String  sessionCleared     = null;
    private boolean allSessionsCleared = false;

    public void reset() {
      opened = false;
      closed = false;
      sessionCleared = null;
      allSessionsCleared = false;
    }

    public void opened() {
      opened = true;
    }

    public void closed() {
      closed = true;
    }

    public void sessionCleared(final String sessionId) {
      sessionCleared = sessionId;
    }

    public void allSessionsCleared() {
      allSessionsCleared = true;
    }
  }
}
