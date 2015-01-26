/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.change;

import com.tc.object.LogicalOperation;
import com.tc.object.TCObject;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalChangeID;

/**
 * @author orion
 */
public interface TCChangeBuffer {

  public boolean isEmpty();

  public void logicalInvoke(LogicalOperation method, Object[] parameters, LogicalChangeID id);

  public void writeTo(DNAWriter writer);

  public TCObject getTCObject();

}
