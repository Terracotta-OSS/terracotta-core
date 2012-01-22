/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

import java.lang.reflect.Field;

/**
 * Interface for the field signature.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
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
