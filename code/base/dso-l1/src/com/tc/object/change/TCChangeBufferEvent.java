/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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