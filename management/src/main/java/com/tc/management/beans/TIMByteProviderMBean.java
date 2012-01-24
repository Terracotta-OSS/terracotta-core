/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans;

import java.io.IOException;

public interface TIMByteProviderMBean {
  public String getManifestEntry(String name);

  public byte[] getResourceAsByteArray(String name) throws IOException;

  public byte[] getModuleBytes() throws Exception;
}
