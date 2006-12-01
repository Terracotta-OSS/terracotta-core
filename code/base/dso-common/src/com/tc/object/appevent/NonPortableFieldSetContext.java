/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.util.NonPortableReason;

public class NonPortableFieldSetContext extends NonPortableEventContext {

  private static final long serialVersionUID = -556002400100752261L;

  private final String  fieldName;
  private final boolean isRoot;

  public NonPortableFieldSetContext(String targetClassName, String threadName,
                                    String clientId, String fieldName, boolean isRoot) {
    super(targetClassName, threadName, clientId);
    this.fieldName = fieldName;
    this.isRoot = isRoot;
  }

  public String getFieldName() {
    return fieldName;
  }

  public boolean isRoot() {
    return isRoot;
  }

  public void addDetailsTo(NonPortableReason reason) {
    super.addDetailsTo(reason);
    if (isRoot()) {
      reason.addDetail("Non-portable root name", fieldName);
    } else {
      reason.addDetail("Non-portable field name", fieldName);
    }
  }

}
