/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.mgmt;


abstract class AbstractObjectFacade implements ManagedObjectFacade {

  AbstractObjectFacade() {
    //
  }

  @Override
  public final String getFieldType(String fieldName) {
    return FacadeUtil.getFieldType(getFieldValue(fieldName));
  }

  @Override
  public final Object getFieldValue(String fieldName) {
    Object value = basicGetFieldValue(fieldName);
    return FacadeUtil.processValue(value);
  }

  protected abstract Object basicGetFieldValue(String fieldName);
}
