/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.bind.serial.ClassCatalog;

public class SleepycatSerializationAdapterFactory implements SerializationAdapterFactory {

  public SerializationAdapter newAdapter(ClassCatalog classCatalog) {
    return new SleepycatSerializationAdapter(classCatalog);
  }

}
