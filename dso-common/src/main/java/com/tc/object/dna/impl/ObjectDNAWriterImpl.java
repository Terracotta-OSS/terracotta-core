/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
