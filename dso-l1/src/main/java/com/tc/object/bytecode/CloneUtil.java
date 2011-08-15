/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;


public class CloneUtil {

  /**
   * This method nulls the TCObject reference in the cloned object if the clone() method had done a shallow copy. This
   * is called from the instrumented code.
   * 
   * @param original Original object
   * @param clone Clone of original
   * @return Clone, modified
   */
  public static Object fixTCObjectReferenceOfClonedObject(Object original, Object clone) {
    if (clone instanceof Manageable && original instanceof Manageable) {
      Manageable mClone = (Manageable) clone;
      Manageable mOriginal = (Manageable) original;
      if (mClone.__tc_managed() != null && clone != original && mClone.__tc_managed() == mOriginal.__tc_managed()) {
        // A shallow copy is returned. We don't want the clone to have the same TCObject
        mClone.__tc_managed(null);
      }
    }
    return clone;
  }

}
