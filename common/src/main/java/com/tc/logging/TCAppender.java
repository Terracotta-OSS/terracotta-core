/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

public interface TCAppender {

  void append(LogLevel level, Object message, Throwable throwable);

}
