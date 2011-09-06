/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.api;

import com.tc.object.metadata.MetaDataDescriptorInternal;

public interface DNAWriterInternal extends DNAWriter {

  void addMetaData(MetaDataDescriptorInternal md);

}
