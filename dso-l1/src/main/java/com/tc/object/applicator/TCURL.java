/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

public interface TCURL {
  public void __tc_set_logical(String protocol, String host, int port, String authority, String userInfo, String path,
                               String query, String ref);
}