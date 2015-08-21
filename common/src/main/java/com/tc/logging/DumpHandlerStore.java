/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import com.tc.handler.CallbackDumpAdapter;

public interface DumpHandlerStore {
  void registerForDump(CallbackDumpAdapter dumpObject);
}
