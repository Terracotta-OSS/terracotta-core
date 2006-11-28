/*
 * Created on Dec 20, 2003
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