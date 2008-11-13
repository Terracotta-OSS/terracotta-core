/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

public class LossyTCLoggerTest extends TCTestCase {

  public void testCountBasedBlindLossyLogger() throws Exception {
    LossyTCLogger lossyLogger = new LossyTCLogger(TCLogging.getConsoleLogger(), 3, LossyTCLogger.COUNT_BASED, false);

    lossyLogger.info("LogMessage-1");
    Assert.assertEquals(1, lossyLogger.getLogCount());
    lossyLogger.info("LogMessage-1");
    lossyLogger.info("LogMessage-1");

    lossyLogger.info("LogMessage-1");
    Assert.assertEquals(2, lossyLogger.getLogCount());
    lossyLogger.info("LogMessage-1");
    lossyLogger.info("LogMessage-1");

    lossyLogger.info("LogMessage-1");
    Assert.assertEquals(3, lossyLogger.getLogCount());

    lossyLogger.info("LogMessage-2");
    lossyLogger.info("LogMessage-1");
    lossyLogger.info("LogMessage-3");
    Assert.assertEquals(4, lossyLogger.getLogCount());

    lossyLogger.info("LogMessage-3");
    lossyLogger.info("LogMessage-3");
    lossyLogger.info("LogMessage-3");
    Assert.assertEquals(5, lossyLogger.getLogCount());
  }

  public void testCountBasedContentCheckLossyLogger() throws Exception {
    LossyTCLogger lossyLogger = new LossyTCLogger(TCLogging.getConsoleLogger(), 3, LossyTCLogger.COUNT_BASED, true);

    lossyLogger.info("LogMessage-1");
    Assert.assertEquals(1, lossyLogger.getLogCount());
    lossyLogger.info("LogMessage-1");
    lossyLogger.info("LogMessage-1");

    lossyLogger.info("LogMessage-1");
    Assert.assertEquals(2, lossyLogger.getLogCount());
    lossyLogger.info("LogMessage-1");
    lossyLogger.info("LogMessage-1");

    lossyLogger.info("LogMessage-1");
    Assert.assertEquals(3, lossyLogger.getLogCount());

    lossyLogger.info("LogMessage-2");
    Assert.assertEquals(4, lossyLogger.getLogCount());

    lossyLogger.info("LogMessage-1");
    Assert.assertEquals(5, lossyLogger.getLogCount());

    lossyLogger.info("LogMessage-3");
    Assert.assertEquals(6, lossyLogger.getLogCount());

    lossyLogger.info("LogMessage-3");
    lossyLogger.info("LogMessage-3");
    lossyLogger.info("LogMessage-3");
    Assert.assertEquals(7, lossyLogger.getLogCount());
  }

  public synchronized void testTimeBasedBlindLossyLogger() throws Exception {
    LossyTCLogger lossyLogger = new LossyTCLogger(TCLogging.getConsoleLogger(), 200, LossyTCLogger.TIME_BASED, false);

    lossyLogger.info("LogMessage-1");
    Assert.assertEquals(1, lossyLogger.getLogCount());
    lossyLogger.info("LogMessage-1");
    lossyLogger.info("LogMessage-1");

    ThreadUtil.reallySleep(205);
    lossyLogger.info("LogMessage-1");
    Assert.assertEquals(2, lossyLogger.getLogCount());
    lossyLogger.info("LogMessage-1");
    lossyLogger.info("LogMessage-1");

    ThreadUtil.reallySleep(205);
    lossyLogger.info("LogMessage-1");
    Assert.assertEquals(3, lossyLogger.getLogCount());

    lossyLogger.info("LogMessage-2");
    lossyLogger.info("LogMessage-1");
    lossyLogger.info("LogMessage-3");
    Assert.assertEquals(3, lossyLogger.getLogCount());

    ThreadUtil.reallySleep(205);
    lossyLogger.info("LogMessage-3");
    lossyLogger.info("LogMessage-3");
    lossyLogger.info("LogMessage-3");
    Assert.assertEquals(4, lossyLogger.getLogCount());
  }

  public synchronized void testTimeBasedContentCheckLossyLogger() throws Exception {
    LossyTCLogger lossyLogger = new LossyTCLogger(TCLogging.getConsoleLogger(), 200, LossyTCLogger.TIME_BASED, true);

    lossyLogger.info("LogMessage-1");
    Assert.assertEquals(1, lossyLogger.getLogCount());
    lossyLogger.info("LogMessage-1");
    lossyLogger.info("LogMessage-1");

    ThreadUtil.reallySleep(205);
    lossyLogger.info("LogMessage-1");
    Assert.assertEquals(2, lossyLogger.getLogCount());
    lossyLogger.info("LogMessage-1");
    lossyLogger.info("LogMessage-1");

    ThreadUtil.reallySleep(205);
    lossyLogger.info("LogMessage-1");
    Assert.assertEquals(3, lossyLogger.getLogCount());

    lossyLogger.info("LogMessage-2");
    Assert.assertEquals(4, lossyLogger.getLogCount());

    lossyLogger.info("LogMessage-1");
    Assert.assertEquals(5, lossyLogger.getLogCount());

    lossyLogger.info("LogMessage-3");
    Assert.assertEquals(6, lossyLogger.getLogCount());

    ThreadUtil.reallySleep(205);
    lossyLogger.info("LogMessage-3");
    lossyLogger.info("LogMessage-3");
    lossyLogger.info("LogMessage-3");
    Assert.assertEquals(7, lossyLogger.getLogCount());
  }
}
