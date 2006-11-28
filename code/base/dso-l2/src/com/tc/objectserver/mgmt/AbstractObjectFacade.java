/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.mgmt;

import java.io.Serializable;

abstract class AbstractObjectFacade implements ManagedObjectFacade, Serializable {

  AbstractObjectFacade() {
    //
  }

  public final String getFieldType(String fieldName) {
    return FacadeUtil.getFieldType(getFieldValue(fieldName));
  }

  public final Object getFieldValue(String fieldName) {
    Object value = basicGetFieldValue(fieldName);
    return FacadeUtil.processValue(value);
  }

  protected abstract Object basicGetFieldValue(String fieldName);
}
