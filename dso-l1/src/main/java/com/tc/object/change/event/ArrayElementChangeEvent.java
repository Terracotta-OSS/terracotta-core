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
package com.tc.object.change.event;

import com.tc.object.ObjectID;
import com.tc.object.change.TCChangeBufferEvent;
import com.tc.object.dna.api.DNAWriter;

public class ArrayElementChangeEvent implements TCChangeBufferEvent {

  private final Object value;
  private final int    index;
  private final int    length;

  public ArrayElementChangeEvent(int index, Object value) {
    this(index, value, -1);
  }

  /**
   * @param index index in the array for the changed element or start index for the subarray
   * @param value new value or copied array for the subarray
   * @param legnth the length of the subarray
   */
  public ArrayElementChangeEvent(int index, Object value, int length) {
    this.index = index;
    this.value = value;
    this.length = length;
  }

  @Override
  public void write(DNAWriter to) {
    if (isSubarray()) {
      to.addSubArrayAction(index, value, length);
    } else {
      to.addArrayElementAction(index, value);
    }
  }

  public Object getValue() {
    return value;
  }

  public int getIndex() {
    return index;
  }

  public boolean isReference() {
    return value instanceof ObjectID;
  }

  public boolean isSubarray() {
    return length != -1;
  }

  public int getLength() {
    return length;
  }
}
