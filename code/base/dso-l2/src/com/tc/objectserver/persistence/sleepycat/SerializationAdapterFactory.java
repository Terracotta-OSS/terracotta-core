/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.bind.serial.ClassCatalog;

import java.io.IOException;

public interface SerializationAdapterFactory {

  public SerializationAdapter newAdapter(ClassCatalog classCatalog)
      throws IOException;

}