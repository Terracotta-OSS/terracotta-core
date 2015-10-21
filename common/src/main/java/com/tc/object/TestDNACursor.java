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
package com.tc.object;

import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;

import java.util.ArrayList;
import java.util.List;

public class TestDNACursor implements DNACursor {
  private final List<Object> actions;
  private int           current = -1;

  public TestDNACursor() {
    this(new ArrayList<Object>());
  }

  public TestDNACursor(List<Object> actions) {
    this.actions = actions;
  }

  public void addEntireArray(Object array) {
    this.actions.add(new PhysicalAction(array));
  }

  public void addPhysicalAction(String addFieldName, Object addObj, boolean isref) {
    this.actions.add(new PhysicalAction(addFieldName, addObj, isref));
  }

  public void addLogicalAction(LogicalOperation method, Object params[]) {
    this.actions.add(new LogicalAction(method, params));
  }

  public void addArrayAction(Object[] objects) {
    this.actions.add(new PhysicalAction(objects));
  }

  public void addLiteralAction(Object value) {
    this.actions.add(new LiteralAction(value));
  }

  @Override
  public boolean next() {
    return this.actions.size() > ++this.current;
  }

  @Override
  public LogicalAction getLogicalAction() {
    return (LogicalAction) this.actions.get(this.current);
  }

  @Override
  public Object getAction() {
    return this.actions.get(this.current);
  }

  @Override
  public PhysicalAction getPhysicalAction() {
    return (PhysicalAction) this.actions.get(this.current);
  }

  @Override
  public boolean next(DNAEncoding encoding) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getActionCount() {
    return this.actions.size();
  }

  @Override
  public void reset() throws UnsupportedOperationException {
    this.current = -1;
  }

  public void addLiteralAction(String field, Object value) {
    // actions.add(new PhysicalAction(field, value));
    this.actions.add(new LiteralAction(value));
  }
}
