/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.expression;

import com.tc.util.FindbugsSuppressWarnings;

/**
 * Helper class to have boolean operation on true / false and null, null is assumed to be undetermined, and "not null"="null"
 * A "false && null" will stay false, but a "true && null" will become undetermined (null).
 * <p/>
 * <p/>
 * This is used when the expression cannot be resolved entirely (early matching, cflow, runtime check residuals)
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur</a>
 */
@FindbugsSuppressWarnings("NP_BOOLEAN_RETURN_NULL")
public abstract class Undeterministic {

  /**
   * And operation
   *
   * @param lhs
   * @param rhs
   * @return
   */
  public static Boolean and(Boolean lhs, Boolean rhs) {
    if (lhs != null && rhs != null) {
      // regular AND
      if (lhs.equals(Boolean.TRUE) && rhs.equals(Boolean.TRUE)) {
        return Boolean.TRUE;
      } else {
        return Boolean.FALSE;
      }
    } else if (lhs != null && lhs.equals(Boolean.FALSE)) {
      // one is undetermined and the other is false, so result is false
      return Boolean.FALSE;
    } else if (rhs != null && rhs.equals(Boolean.FALSE)) {
      // one is undetermined and the other is false, so result is false
      return Boolean.FALSE;
    } else {
      // both are undetermined, or one is true and the other undetermined
      return null;
    }
  }

  /**
   * Or operation
   *
   * @param lhs
   * @param rhs
   * @return
   */
  public static Boolean or(Boolean lhs, Boolean rhs) {
    if (lhs != null && rhs != null) {
      // regular OR
      if (lhs.equals(Boolean.TRUE) || rhs.equals(Boolean.TRUE)) {
        return Boolean.TRUE;
      } else {
        return Boolean.FALSE;
      }
    } else {
      // one or both is/are undetermined
      // OR cannot be resolved
      return null;
    }
  }

  /**
   * Not operation
   *
   * @param b
   * @return
   */
  public static Boolean not(Boolean b) {
    if (b != null) {
      // regular NOT
      if (b.equals(Boolean.TRUE)) {
        return Boolean.FALSE;
      } else {
        return Boolean.TRUE;
      }
    } else {
      return null;
    }
  }


}
