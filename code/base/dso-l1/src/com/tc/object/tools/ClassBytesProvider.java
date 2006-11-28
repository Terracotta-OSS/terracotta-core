/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.tools;

public interface ClassBytesProvider {

  byte[] getBytesForClass(String className) throws ClassNotFoundException;

}
