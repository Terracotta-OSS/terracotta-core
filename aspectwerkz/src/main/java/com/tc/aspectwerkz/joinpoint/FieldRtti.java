/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

import java.lang.reflect.Field;

/**
 * Interface for the field RTTI (Runtime Type Information).
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public interface FieldRtti extends MemberRtti {
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

  /**
   * Returns the value of the field.
   *
   * @return the value of the field
   */
  Object getFieldValue();
}
