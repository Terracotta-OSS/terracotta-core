/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;

import java.util.ArrayList;
import java.util.List;

public class TestDNACursor implements DNACursor {
  private final List actions;
  private int        current = -1;

  public TestDNACursor() {
    this(new ArrayList());
  }

  public TestDNACursor(final List actions) {
    this.actions = actions;
  }

  public void addEntireArray(final Object array) {
    this.actions.add(new PhysicalAction(array));
  }

  public void addPhysicalAction(final String addFieldName, final Object addObj, final boolean isref) {
    this.actions.add(new PhysicalAction(addFieldName, addObj, isref));
  }

  public void addLogicalAction(final int method, final Object params[]) {
    this.actions.add(new LogicalAction(method, params));
  }

  public void addArrayAction(final Object[] objects) {
    this.actions.add(new PhysicalAction(objects));
  }

  public void addLiteralAction(final Object value) {
    this.actions.add(new LiteralAction(value));
  }

  public boolean next() {
    return this.actions.size() > ++this.current;
  }

  public LogicalAction getLogicalAction() {
    return (LogicalAction) this.actions.get(this.current);
  }

  public Object getAction() {
    return this.actions.get(this.current);
  }

  public PhysicalAction getPhysicalAction() {
    return (PhysicalAction) this.actions.get(this.current);
  }

  public boolean next(final DNAEncoding encoding) {
    throw new ImplementMe();
  }

  public int getActionCount() {
    return this.actions.size();
  }

  public void reset() throws UnsupportedOperationException {
    this.current = -1;
  }

  public void addLiteralAction(final String field, final Object value) {
    // actions.add(new PhysicalAction(field, value));
    this.actions.add(new LiteralAction(value));
  }
}