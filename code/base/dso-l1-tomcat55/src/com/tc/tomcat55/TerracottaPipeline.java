/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.tomcat55;

import org.apache.catalina.Container;
import org.apache.catalina.Valve;
import org.apache.catalina.core.StandardPipeline;

import com.tc.tomcat55.session.SessionValve55;

public class TerracottaPipeline extends StandardPipeline {

  private final SessionValve55 tcValve;

  public TerracottaPipeline(Container container) {
    super(container);
    tcValve = new SessionValve55();
    addValve(tcValve);
  }

  public void removeValve(Valve valve) {
    if (valve == tcValve) { throw new IllegalArgumentException("Cannot remove the terracotta session valve"); }
    super.removeValve(valve);
  }

}
