/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCSerializable;

import java.util.List;

public interface MetaDataDescriptorInternal extends MetaDataDescriptor, TCSerializable {

  List<AbstractNVPair> getMetaDatas();

}
