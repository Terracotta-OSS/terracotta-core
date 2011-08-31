/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.change;

import com.tc.object.dna.api.DNAWriter;

/**
 * TCChangeBufferEvents are discreet changes to a managed object
 * 
 * @author orion
 */
public interface TCChangeBufferEvent {
  
  public void write(DNAWriter to);
  
}