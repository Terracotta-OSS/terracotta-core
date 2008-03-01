/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

import java.io.Writer;


public interface DumpHandler {

  String dump();

  void dump(Writer writer);
  
  void dumpToLogger();

}
