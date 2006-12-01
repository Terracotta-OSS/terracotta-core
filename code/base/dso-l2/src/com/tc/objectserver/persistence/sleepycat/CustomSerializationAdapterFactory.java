/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.bind.serial.ClassCatalog;
import com.tc.io.serializer.impl.StringUTFSerializer;
import com.tc.objectserver.managedobject.ManagedObjectSerializer;
import com.tc.objectserver.managedobject.ManagedObjectStateSerializer;

public class CustomSerializationAdapterFactory implements SerializationAdapterFactory {

  public SerializationAdapter newAdapter(ClassCatalog classCatalog) {
    ManagedObjectStateSerializer stateSerializer = new ManagedObjectStateSerializer();
    ManagedObjectSerializer moSerializer = new ManagedObjectSerializer(stateSerializer);
    StringUTFSerializer stringSerializer = new StringUTFSerializer();
    return new CustomSerializationAdapter(moSerializer, stringSerializer);
  }
}
