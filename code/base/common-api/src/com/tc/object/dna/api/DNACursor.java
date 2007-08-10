/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.dna.api;

import java.io.IOException;

/**
 * A ResultSet-like interface for iterating over the fields of a DNA strand.
 * <p>
 * TODO: Perhaps this could be better integrated into some Class hierarchy.
 *
 * @author orion
 */
public interface DNACursor {

  public int getActionCount();

  public boolean next() throws IOException;

  public boolean next(DNAEncoding encoding) throws IOException, ClassNotFoundException;

  public void reset() throws UnsupportedOperationException;

  public LogicalAction getLogicalAction();

  public PhysicalAction getPhysicalAction();

  // XXX: This should be removed or cleaned up at some point. It's here to support TreeMap which is treated logically,
  // except for it's "comparator" field which is treated physically
  public Object getAction();

}