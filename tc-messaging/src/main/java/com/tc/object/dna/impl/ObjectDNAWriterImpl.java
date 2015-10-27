/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object.dna.impl;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.EntityID;
import com.tc.object.dna.api.DNAEncodingInternal;

public class ObjectDNAWriterImpl extends DNAWriterImpl {

  public ObjectDNAWriterImpl(TCByteBufferOutputStream output, EntityID id, String className,
                             ObjectStringSerializer serializer, DNAEncodingInternal encoding, long version,
                             boolean isDelta) {
    super(output, id, serializer, encoding, version, isDelta);
  }

}
