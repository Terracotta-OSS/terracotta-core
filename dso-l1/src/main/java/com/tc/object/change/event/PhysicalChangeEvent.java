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

public class PhysicalChangeEvent implements TCChangeBufferEvent {
  private final Object newValue;
  private final String fieldname;

  public PhysicalChangeEvent(String fieldname, Object newValue) {
    this.newValue = newValue;
    this.fieldname = fieldname;
  }

  public String getFieldName() {
    return fieldname;
  }

  public Object getNewValue() {
    return newValue;
  }

  public boolean isReference() {
    return newValue instanceof ObjectID;
  }

  @Override
  public void write(DNAWriter writer) {
    writer.addPhysicalAction(fieldname, newValue);
  }

}