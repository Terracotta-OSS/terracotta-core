/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.tcconfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SpringTerracottaAppServerConfig extends StandardTerracottaAppServerConfig {

  byte[] data;
  
  public SpringTerracottaAppServerConfig(File config) throws IOException {
    super(new File(""));
    FileInputStream in = new FileInputStream(config);
    data = new byte[in.available()];
    in.read(data);
    in.close();
  }
  
  public String toString() {
    return new String(data);
  }
}
