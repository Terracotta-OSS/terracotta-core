/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.tomcat55;

import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import com.tc.tomcat55.session.SessionValve55;

import junit.framework.TestCase;

public class TerracottaPipelineTest extends TestCase {

  public void testValve() {
    TerracottaPipeline pipeline = new TerracottaPipeline(null);
    pipeline.getValveObjectNames(); // call this just to make sure it works with our valve
    Valve[] valves = pipeline.getValves();

    assertEquals(1, valves.length);

    // cast will fail it is some other type
    SessionValve55 valve = (SessionValve55) valves[0];

    try {
      pipeline.removeValve(valve);
      fail();
    } catch (IllegalArgumentException iae) {
      // exptected
    }

    // mutating the array doesn't affect the pipeline
    valves[0] = null;
    assertEquals(valve, pipeline.getValves()[0]);

    pipeline.addValve(new DummyValve());
    assertEquals(2, pipeline.getValves().length);
    assertEquals(valve, pipeline.getValves()[0]);
  }

  private static class DummyValve extends ValveBase {

    public void invoke(Request arg0, Response arg1) {
      //
    }

  }

}
