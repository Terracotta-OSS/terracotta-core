/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

/*
 * Interface for objects which can be linked in a determinable order
 */
public interface TCLinkable<T extends TCLinkable> {
  T getNext();

  void setNext(T n);

  T getPrevious();

  void setPrevious(T n);

}
