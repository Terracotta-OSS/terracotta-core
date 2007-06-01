/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.util.NonPortableReason;

public class NonPortableRootContext extends NonPortableEventContext {

  private static final long serialVersionUID = -556002400100752261L;

  private final String      rootName;

  public NonPortableRootContext(String threadName, String clientId, String rootName, Object rootValue) {
    super(rootValue, threadName, clientId);
    this.rootName = rootName;
  }

  public String getFieldName() {
    return rootName;
  }

  public Object getFieldValue() {
    return getPojo();
  }

  public void addDetailsTo(NonPortableReason reason) {
    super.addDetailsTo(reason);
    reason.addDetail("Non-portable root name", rootName);
  }

}
