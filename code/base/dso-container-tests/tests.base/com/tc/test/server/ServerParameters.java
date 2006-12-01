/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server;

/**
 * Arguments passed to a server to be utilized in it's initialization.
 */
public interface ServerParameters {

  String jvmArgs();

  String classpath();
}