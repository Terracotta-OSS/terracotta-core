/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tctest.spring.integrationtests.framework.Server;

public interface TestServerCallback {

  void test(Server tomcatServer) throws Exception;

}
