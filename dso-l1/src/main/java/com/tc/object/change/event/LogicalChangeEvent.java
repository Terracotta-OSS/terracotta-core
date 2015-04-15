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

import com.tc.object.LogicalOperation;
import com.tc.object.change.TCChangeBufferEvent;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalChangeID;

/**
 * Nov 22, 2004: Event representing any logical actions that need to be logged
 */
public class LogicalChangeEvent implements TCChangeBufferEvent {
  private final LogicalOperation method;
  private final Object[] parameters;
  private final LogicalChangeID logicalChangeID;

  public LogicalChangeEvent(LogicalOperation method, Object[] parameters, LogicalChangeID id) {
    this.parameters = parameters;
    this.method = method;
    this.logicalChangeID = id;
  }

  @Override
  public void write(DNAWriter writer) {
    writer.addLogicalAction(method, parameters, logicalChangeID);
  }

  public LogicalOperation getLogicalOperation() {
    return method;
  }

  public Object[] getParameters() {
    return parameters;
  }

  public LogicalChangeID getLogicalChangeID(){
    return this.logicalChangeID;
  }

}