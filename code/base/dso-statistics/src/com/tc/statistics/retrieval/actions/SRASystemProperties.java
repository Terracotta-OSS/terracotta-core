/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.exception.TCRuntimeException;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

public class SRASystemProperties implements StatisticRetrievalAction {

  public final static String ACTION_NAME = "system properties";

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.STARTUP;
  }

  public StatisticData[] retrieveStatisticData() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Properties properties = System.getProperties();
    try {
      properties.store(out, null);
      return new StatisticData[] { new StatisticData(ACTION_NAME, new Date(), out.toString("ISO-8859-1")) };
    } catch (IOException e) {
      throw new TCRuntimeException("Unexpected error while storing the system properties.", e);
    }
  }
}