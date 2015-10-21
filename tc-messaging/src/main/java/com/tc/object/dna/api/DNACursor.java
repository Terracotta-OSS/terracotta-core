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
package com.tc.object.dna.api;

import java.io.IOException;

/**
 * A ResultSet-like interface for iterating over the fields of a DNA strand.  Generally the set 
 * of actions to cursor over consist of either all logical or all physical depending on the class.
 * <p>
 * TODO: Perhaps this could be better integrated into some Class hierarchy.
 *
 * @author orion
 */
public interface DNACursor {

  /**
   * Get total number of actions
   * @return Action count
   */
  public int getActionCount();

  /**
   * Move to next action
   * @return True if there is a next action, false if no more actions
   * @throws IOException If an IO error occurs while moving the cursor
   */
  public boolean next() throws IOException;

  /**
   * Move to next action and use specific encoding
   * @param encoding The DNA encoding
   * @return True if there is a next action, false if no more actions
   * @throws IOException If an IO error occurs while moving the cursor
   * @throws ClassNotFoundException If a class is not found while deserializing DNA
   */
  public boolean next(DNAEncoding encoding) throws IOException, ClassNotFoundException;

  /**
   * Reset the cursor
   * @throws UnsupportedOperationException If this cursor implementation does not support reset()
   */
  public void reset() throws UnsupportedOperationException;

  /**
   * Get a logical action at the current cursor location.
   * @return Logical action
   */
  public LogicalAction getLogicalAction();

  /**
   * Get a physical action at the current cursor location.
   * @return Physical action
   */
  public PhysicalAction getPhysicalAction();

  /**
   * Return the action at the current cursor location.
   * 
   * XXX: This should be removed or cleaned up at some point. It's here to support TreeMap which is treated logically,
   * except for it's "comparator" field which is treated physically
   * @return LogicalAction, PhysicalAction, or LiteralAction
   */
  public Object getAction();

}