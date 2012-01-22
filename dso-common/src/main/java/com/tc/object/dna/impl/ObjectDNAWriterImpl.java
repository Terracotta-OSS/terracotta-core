/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNAEncodingInternal;

public class ObjectDNAWriterImpl extends DNAWriterImpl {

  public ObjectDNAWriterImpl(TCByteBufferOutputStream output, ObjectID id, String className,
                             ObjectStringSerializer serializer, DNAEncodingInternal encoding, String loaderDesc,
                             long version, boolean isDelta) {
    super(output, id, className, serializer, encoding, loaderDesc, version, isDelta);
  }

}
