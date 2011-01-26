/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCSerializable;
import com.tc.object.ObjectID;

import java.util.Iterator;

public interface MetaDataDescriptorInternal extends MetaDataDescriptor, TCSerializable {

  Iterator<NVPair> getMetaDatas();

  int numberOfNvPairs();

  ObjectID getObjectId();

  void setObjectID(ObjectID id);

}
