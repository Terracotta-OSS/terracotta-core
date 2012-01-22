/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.util.NonPortableReason;

/**
 * The event context for a set of a non-portable field event.
 */
public class NonPortableFieldSetContext extends NonPortableEventContext {

  private static final long serialVersionUID = -556002400100752262L;

  private final String           fieldName;
  private transient final Object fieldValue;

  public static final String     FIELD_NAME_LABEL = "Non-portable field name";

  public NonPortableFieldSetContext(String threadName, String clientId, Object pojo, String fieldName, Object fieldValue) {
    super(pojo, threadName, clientId);
    this.fieldName = fieldName;
    this.fieldValue = fieldValue;
  }

  /**
   * @return The field name being set
   */
  public String getFieldName() {
    return fieldName;
  }

  /**
   * @return The field value being set
   */
  public Object getFieldValue() {
    return fieldValue;
  }

  public void addDetailsTo(NonPortableReason reason) {
    super.addDetailsTo(reason);
    reason.addDetail(FIELD_NAME_LABEL, fieldName);
  }

}
