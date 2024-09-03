/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
      curs.limit(Math.min(curs.position() + runTo, curs.limit()));
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
