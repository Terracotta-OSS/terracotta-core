/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.statistics;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticDataCSVParser;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;

public class StatisticDataTest extends TestCase {
  public void testDefaultInstantiation() throws Exception {
    StatisticData data = new StatisticData();
    assertNull(data.getSessionId());
    assertNull(data.getAgentIp());
    assertNull(data.getMoment());
    assertNull(data.getName());
    assertNull(data.getElement());
    assertNull(data.getData());
  }

  public void testDefaultToString() throws Exception {
    StatisticData data = new StatisticData();
    assertEquals(
                 "[sessionId = null; agentIp = null; agentDifferentiator = null; moment = null; name = null; element = null; data = null]",
                 data.toString());
  }

  public void testDefaultToCsv() throws Exception {
    StatisticData data = new StatisticData();
    assertEquals(",,,,,,,,,\n", data.toCsv());
  }

  public void testDefaultToLog() throws Exception {
    StatisticData data = new StatisticData();
    assertEquals("null : null", data.toLog());
  }

  public void testFluentInterface() throws Exception {
    Date moment = new Date();
    StatisticData data = new StatisticData().sessionId("3984693").agentIp(InetAddress.getLocalHost().getHostAddress())
        .agentDifferentiator("blurb").moment(moment).name("statname").element("first").data(new Long(987983343L));

    assertEquals("3984693", data.getSessionId());
    assertEquals(InetAddress.getLocalHost().getHostAddress(), data.getAgentIp());
    assertEquals("blurb", data.getAgentDifferentiator());
    assertEquals(moment, data.getMoment());
    assertEquals("statname", data.getName());
    assertEquals("first", data.getElement());
    assertEquals(new Long(987983343L), data.getData());

    data.data("datastring");
    assertEquals("datastring", data.getData());

    Date dataDate = new Date();
    data.data(dataDate);
    assertEquals(dataDate, data.getData());
  }

  public void testSetters() throws Exception {
    Date moment = new Date();
    StatisticData data = new StatisticData();
    data.setSessionId("3984693");
    data.setAgentIp(InetAddress.getLocalHost().getHostAddress());
    data.setAgentDifferentiator("blurb");
    data.setMoment(moment);
    data.setName("statname");
    data.setElement("first");
    data.setData(new Long(987983343L));

    assertEquals("3984693", data.getSessionId());
    assertEquals(InetAddress.getLocalHost().getHostAddress(), data.getAgentIp());
    assertEquals("blurb", data.getAgentDifferentiator());
    assertEquals(moment, data.getMoment());
    assertEquals("statname", data.getName());
    assertEquals("first", data.getElement());
    assertEquals(new Long(987983343L), data.getData());

    data.setData("datastring");
    assertEquals("datastring", data.getData());

    Date dataDate = new Date();
    data.setData(dataDate);
    assertEquals(dataDate, data.getData());

    data.setData(new BigDecimal("343.1778"));
    assertEquals(new BigDecimal("343.1778"), data.getData());
  }

  public void testToString() throws Exception {
    Calendar moment = Calendar.getInstance();
    moment.set(2008, 0, 9, 16, 25, 52);
    moment.set(Calendar.MILLISECOND, 0);
    StatisticData data = new StatisticData().sessionId("3984693").agentIp("192.168.1.18").agentDifferentiator("7826")
        .moment(moment.getTime()).name("statname").element("first").data(new Long(987983343L));
    assertEquals(
                 "[sessionId = 3984693; agentIp = 192.168.1.18; agentDifferentiator = 7826; moment = 01/09/2008 16:25:52 000; name = statname; element = first; data = 987983343]",
                 data.toString());
  }

  public void testToCsv() throws Exception {
    Calendar moment = Calendar.getInstance();
    moment.set(2008, 0, 9, 16, 25, 52);
    moment.set(Calendar.MILLISECOND, 0);

    StatisticData data1 = new StatisticData().sessionId("3984693").agentIp("192.168.1.18").agentDifferentiator("7826")
        .moment(moment.getTime()).name("statname").element("first").data(new Long(987983343L));
    assertEquals("\"3984693\",\"192.168.1.18\",\"7826\",\"" + moment.getTime().getTime()
                 + "\",\"statname\",\"first\",\"987983343\",,,\n", data1.toCsv());

    StatisticData data2 = new StatisticData().sessionId("3984693").agentIp("192.168.1.18").agentDifferentiator("7826")
        .moment(moment.getTime()).name("statname").element("first").data("t\\\nex\rt\nd\"a\\\"ta\\");
    assertEquals("\"3984693\",\"192.168.1.18\",\"7826\",\"" + moment.getTime().getTime()
                 + "\",\"statname\",\"first\",,\"t\\\\\\next\\nd\\\"a\\\\\\\"ta\\\\\",,\n", data2.toCsv());

    StatisticData data3 = new StatisticData().sessionId("3984693").agentIp("192.168.1.18").agentDifferentiator("7826")
        .moment(moment.getTime()).name("statname").element("first").data(moment.getTime());
    assertEquals("\"3984693\",\"192.168.1.18\",\"7826\",\"" + moment.getTime().getTime()
                 + "\",\"statname\",\"first\",,,\"" + moment.getTime().getTime() + "\",\n", data3.toCsv());

    StatisticData data4 = new StatisticData().sessionId("3984693").agentIp("192.168.1.18").agentDifferentiator("7826")
        .moment(moment.getTime()).name("statname").element("first").data(new BigDecimal("268.75862"));
    assertEquals("\"3984693\",\"192.168.1.18\",\"7826\",\"" + moment.getTime().getTime()
                 + "\",\"statname\",\"first\",,,,\"268.75862\"\n", data4.toCsv());
  }

  public void testToLog() throws Exception {
    Calendar moment = Calendar.getInstance();
    moment.set(2008, 0, 9, 16, 25, 52);
    moment.set(Calendar.MILLISECOND, 0);
    StatisticData data = new StatisticData().sessionId("3984693").agentIp("192.168.1.18").agentDifferentiator("7826")
        .moment(moment.getTime()).name("statname").element("first").data(new Long(987983343L));
    assertEquals("statname - first : 987983343", data.toLog());
    data.element(null);
    assertEquals("statname : 987983343", data.toLog());
    data.data(moment.getTime());
    assertEquals("statname : 01/09/2008 16:25:52 000", data.toLog());
  }

  public void testFromCsvUnsupportedVersion() throws Exception {
    try {
      StatisticDataCSVParser.newInstanceFromCsvLine("unknown", ",,,,,,,,,\n");
      fail("expected exception");
    } catch (ParseException e) {
      // expected
    }
  }

  public void testFromCsv() throws Exception {
    Calendar moment = Calendar.getInstance();
    moment.set(2008, 0, 9, 16, 25, 52);
    moment.set(Calendar.MILLISECOND, 0);

    StatisticData data1 = new StatisticData().sessionId("3984693").agentIp("192.168.1.18").moment(moment.getTime())
        .name("statname").element("first").data(new Long(987983343L));
    assertEquals(data1.toString(), StatisticDataCSVParser
        .newInstanceFromCsvLine(StatisticDataCSVParser.CURRENT_CSV_VERSION, data1.toCsv()).toString());
    assertTrue(data1.getData() instanceof Long);

    StatisticData data2 = new StatisticData().sessionId("3984693").agentIp("192.168.1.18").agentDifferentiator("7826")
        .moment(moment.getTime()).element("first").data("t\\\next\nd\"a\\\"ta\\");
    assertEquals(data2.toString(), StatisticDataCSVParser
        .newInstanceFromCsvLine(StatisticDataCSVParser.CURRENT_CSV_VERSION, data2.toCsv()).toString());
    assertTrue(data2.getData() instanceof String);

    StatisticData data3 = new StatisticData().sessionId("3984693").agentIp("192.168.1.18").agentDifferentiator("7826")
        .moment(moment.getTime()).name("statname").data(moment.getTime());
    assertEquals(data3.toString(), StatisticDataCSVParser
        .newInstanceFromCsvLine(StatisticDataCSVParser.CURRENT_CSV_VERSION, data3.toCsv()).toString());
    assertTrue(data3.getData() instanceof Date);

    StatisticData data4 = new StatisticData().sessionId("3984693").agentIp("192.168.1.18").agentDifferentiator("7826")
        .name("statname").element("first").data(new BigDecimal("268.75862"));
    assertEquals(data4.toString(), StatisticDataCSVParser
        .newInstanceFromCsvLine(StatisticDataCSVParser.CURRENT_CSV_VERSION, data4.toCsv()).toString());
    assertTrue(data4.getData() instanceof BigDecimal);
  }
}
