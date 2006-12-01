/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tctest.spring.integrationtests.framework.Server;

public interface TestServerCallback {

  void test(Server tomcatServer) throws Exception;

}
