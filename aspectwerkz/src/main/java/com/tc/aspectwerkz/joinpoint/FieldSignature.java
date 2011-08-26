/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

import java.lang.reflect.Field;

/**
 * Interface for the field signature.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public interface FieldSignature extends MemberSignature {
  /**
   * Returns the field.
   *
   * @return the field
   */
  Field getField();

  /**
   * Returns the field type.
   *
   * @return the field type
   */
  Class getFieldType();
}