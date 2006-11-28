/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.test.server.appserver.war;

/**
 * Represents additional XML descriptor files to be included in the generated WAR.
 */
public interface DescriptorXml {
  
  byte[] getBytes();

  String getFileName();
}
