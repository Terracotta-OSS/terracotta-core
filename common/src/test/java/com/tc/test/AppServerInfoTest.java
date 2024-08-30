/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.test;

import junit.framework.TestCase;

public class AppServerInfoTest extends TestCase {

  public final void testParse() {
    AppServerInfo appServer = AppServerInfo.parse("tomcat-5.5.26");
    assertEquals("tomcat", appServer.getName());
    assertEquals("5", appServer.getMajor());
    assertEquals("5.26", appServer.getMinor());
    assertEquals(AppServerInfo.TOMCAT, appServer.getId());

    appServer = AppServerInfo.parse("weblogic-9.2.mp2");
    assertEquals("weblogic", appServer.getName());
    assertEquals("9", appServer.getMajor());
    assertEquals("2.mp2", appServer.getMinor());
    assertEquals(AppServerInfo.WEBLOGIC, appServer.getId());

    appServer = AppServerInfo.parse("jboss-eap-6.1.0");
    assertEquals("jboss-eap", appServer.getName());
    assertEquals("6", appServer.getMajor());
    assertEquals("1.0", appServer.getMinor());
    assertEquals(AppServerInfo.JBOSS, appServer.getId());

    IllegalArgumentException exception = null;
    try {
      AppServerInfo.parse("invalid-appserver-specification.1");
    }
    catch (IllegalArgumentException e) {
      exception = e;
    }
    assertNotNull(exception);
  }

  public final void testToString() {
    String nameAndVersion = "jboss-eap-6.1.0";
    assertEquals(nameAndVersion, AppServerInfo.parse(nameAndVersion).toString());
  }

}
