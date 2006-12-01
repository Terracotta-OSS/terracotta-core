/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.war;

/**
 * Represents additional XML descriptor files to be included in the generated WAR.
 */
public interface DescriptorXml {
  
  byte[] getBytes();

  String getFileName();
}
