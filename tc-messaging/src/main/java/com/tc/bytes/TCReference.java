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
package com.tc.bytes;

import com.tc.util.Assert;
import java.util.Iterator;
import java.util.stream.StreamSupport;

/**
 *
 */
public interface TCReference extends Iterable<TCByteBuffer>, AutoCloseable {
  
  TCReference duplicate();
  
  default TCReference duplicate(int length) {
    return duplicate().truncate(length);
  }
  
  default TCReference truncate(int length) {
    Iterator<TCByteBuffer> it = iterator();
    int runTo = length;
    while (it.hasNext()) {
      TCByteBuffer curs = it.next();
      Assert.assertEquals(0, curs.position());
      curs.limit(Math.min(runTo, curs.limit()));
      runTo -= curs.remaining();
    }
    return this;
  }
  
  default int available() {
    return StreamSupport.stream(spliterator(), false).map(TCByteBuffer::remaining).reduce(0, Integer::sum);
  }

  @Override
  public void close();
}
