/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.util.sequence;

public class ObjectIDSequenceProvider implements ObjectIDSequence {

  private long current;

  public ObjectIDSequenceProvider(long start) {
    this.current = start;
  }

  @Override
  public synchronized long nextObjectIDBatch(int batchSize) {
    final long start = current;
    current += batchSize;
    return start;
  }

  @Override
  public void setNextAvailableObjectID(long startID) {
    if (current > startID) { throw new AssertionError("Current value + " + current + " is greater than " + startID); }
    current = startID;
  }

  @Override
  public long currentObjectIDValue() {
    return this.current;
  }

}
