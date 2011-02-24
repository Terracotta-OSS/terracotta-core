/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;

import java.util.Iterator;

public interface MetaDataDescriptorInternal extends MetaDataDescriptor {

  Iterator<NVPair> getMetaDatas();

  int numberOfNvPairs();

  ObjectID getObjectId();

  void setObjectID(ObjectID id);

  void serializeTo(TCByteBufferOutput output, ObjectStringSerializer serializer);

}
