/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcverify;

import java.util.ArrayList;
import java.util.Collection;

public class DSOVerifierCDV810ProtocolHandlersTest extends DSOVerifierTest {

  protected String getMainClass() {
    return getClass().getName();
  }

  protected Collection<String> getExtraJvmArgs() {
    Collection<String> rv = new ArrayList<String>();
    rv.add("-Djava.protocol.handler.pkgs=com.sun.net.ssl.internal.www.protocol");
    return rv;
  }

  public static void main(String[] args) {
    DSOVerifier.main(args);
  }

}