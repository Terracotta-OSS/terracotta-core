/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

public interface TCLoggingService {

  TCLogger getLogger(String name);

  TCLogger getLogger(Class c);


}
