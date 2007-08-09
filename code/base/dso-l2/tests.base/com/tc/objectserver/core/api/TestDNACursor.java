/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.exception.ImplementMe;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.IDNAEncoding;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;

import java.util.ArrayList;
import java.util.List;

/**
 * @author steve
 */
public class TestDNACursor implements DNACursor {

  private List   actions = new ArrayList();
  private int current = -1;

  public void addPhysicalAction(String addFieldName, Object addObj) {
    actions.add(new PhysicalAction(addFieldName, addObj, true));
  }
  
  public void addPhysicalAction(String addFieldName, Object addObj, boolean isref) {
    actions.add(new PhysicalAction(addFieldName, addObj, isref));
  }
  
  public void addLogicalAction(int method, Object params[]) {
    actions.add(new LogicalAction(method, params));
  }
  
  public void addArrayAction(Object[] objects) {
    actions.add(new PhysicalAction(objects));
  }

  public void addLiteralAction(String field, Object value) {
    //actions.add(new PhysicalAction(field, value));
    actions.add(new LiteralAction(value));
  }

  public boolean next() {
    return actions.size() > ++current;
  }

  public LogicalAction getLogicalAction() {
    return (LogicalAction) actions.get(current);
  }

  public Object getAction() {
    return actions.get(current);
  }

  public PhysicalAction getPhysicalAction() {
    return (PhysicalAction) actions.get(current);
  }

  public boolean next(IDNAEncoding encoding) {
    throw new ImplementMe();
  }

  public int getActionCount() {
    return actions.size();
  }

  public void reset() throws UnsupportedOperationException {
    current = -1;
  }

}