/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics.store;

import com.tc.statistics.store.StatisticsRetrievalCriteria;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;

public class StatisticsRetrievalCriteriaTests extends TestCase {
  public void testDefaultInstantiation() throws Exception {
    StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria();
    assertNull(criteria.getAgentIp());
    assertNull(criteria.getSessionId());
    assertNull(criteria.getStart());
    assertNull(criteria.getStop());
    assertEquals(0, criteria.getNames().size());
    assertEquals(0, criteria.getElements().size());
  }

  public void testDefaultToString() throws Exception {
    StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria();
    assertEquals("[agentIp = null; sessionId = null; start = null; stop = null; names = []; elements = []]", criteria.toString());
  }

  public void testFluentInterface() throws Exception {
    Calendar cal1 = Calendar.getInstance();
    cal1.set(2008, 0, 9, 16, 25, 52);
    cal1.set(Calendar.MILLISECOND, 0);

    Calendar cal2 = Calendar.getInstance();
    cal2.set(2008, 1, 2, 15, 22, 13);
    cal2.set(Calendar.MILLISECOND, 0);

    Date moment1 = cal1.getTime();
    Date moment2 = cal2.getTime();
    assertFalse(moment1.equals(moment2));

    StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria()
      .agentIp(InetAddress.getLocalHost().getHostAddress())
      .sessionId("423")
      .start(moment1)
      .stop(moment2)
      .addName("statname1")
      .addName("statname2")
      .addName("statname3")
      .addElement("element1")
      .addElement("element2");

    assertEquals(InetAddress.getLocalHost().getHostAddress(), criteria.getAgentIp());
    assertEquals("423", criteria.getSessionId());
    assertEquals(moment1, criteria.getStart());
    assertEquals(moment2, criteria.getStop());
    assertEquals(3, criteria.getNames().size());
    assertTrue(criteria.getNames().contains("statname1"));
    assertTrue(criteria.getNames().contains("statname2"));
    assertTrue(criteria.getNames().contains("statname3"));
    assertEquals(2, criteria.getElements().size());
    assertTrue(criteria.getElements().contains("element1"));
    assertTrue(criteria.getElements().contains("element2"));
  }

  public void testSetters() throws Exception {
    Calendar cal1 = Calendar.getInstance();
    cal1.set(2008, 0, 9, 16, 25, 52);
    cal1.set(Calendar.MILLISECOND, 0);

    Calendar cal2 = Calendar.getInstance();
    cal2.set(2008, 1, 2, 15, 22, 13);
    cal2.set(Calendar.MILLISECOND, 0);

    Date moment1 = cal1.getTime();
    Date moment2 = cal2.getTime();
    assertFalse(moment1.equals(moment2));

    StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria();
    criteria.setAgentIp(InetAddress.getLocalHost().getHostAddress());
    criteria.setSessionId("423");
    criteria.setStart(moment1);
    criteria.setStop(moment2);
    criteria.addName("statname1");
    criteria.addName("statname2");
    criteria.addName("statname3");
    criteria.addElement("element1");
    criteria.addElement("element2");

    assertEquals(InetAddress.getLocalHost().getHostAddress(), criteria.getAgentIp());
    assertEquals("423", criteria.getSessionId());
    assertEquals(moment1, criteria.getStart());
    assertEquals(moment2, criteria.getStop());
    assertEquals(3, criteria.getNames().size());
    assertTrue(criteria.getNames().contains("statname1"));
    assertTrue(criteria.getNames().contains("statname2"));
    assertTrue(criteria.getNames().contains("statname3"));
    assertEquals(2, criteria.getElements().size());
    assertTrue(criteria.getElements().contains("element1"));
    assertTrue(criteria.getElements().contains("element2"));
  }

  public void testToString() throws Exception {
    Calendar cal1 = Calendar.getInstance();
    cal1.set(2008, 0, 9, 16, 25, 52);
    cal1.set(Calendar.MILLISECOND, 0);

    Calendar cal2 = Calendar.getInstance();
    cal2.set(2008, 1, 2, 15, 22, 13);
    cal2.set(Calendar.MILLISECOND, 0);

    Date moment1 = cal1.getTime();
    Date moment2 = cal2.getTime();
    assertFalse(moment1.equals(moment2));

    StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria()
      .agentIp(InetAddress.getLocalHost().getHostAddress())
      .sessionId("423")
      .start(moment1)
      .stop(moment2)
      .addName("statname1")
      .addName("statname2")
      .addName("statname3")
      .addElement("element1")
      .addElement("element2");

    assertEquals("[agentIp = 192.168.1.43; sessionId = 423; start = 2008-01-09 16:25:52 000; stop = 2008-02-02 15:22:13 000; names = [statname1, statname2, statname3]; elements = [element1, element2]]", criteria.toString());
  }
}
