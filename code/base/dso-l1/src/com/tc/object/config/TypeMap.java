/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.util.ClassUtils;
import com.tc.util.ClassUtils.ClassSpec;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a set of Type objects from a set of field names.
 */
public class TypeMap {
  private final Map types = new HashMap();
  
  public TypeMap(String[] fieldNames) throws ParseException {
    for (int i=0; i<fieldNames.length; i++) {
      ClassSpec spec = ClassUtils.parseFullyQualifiedFieldName(fieldNames[i]);
      addField(spec.getFullyQualifiedClassName(), spec.getShortFieldName());
    }
  }

  private void addField(String className, String fieldName) {
    Type type = (Type) types.get(className);
    if (type == null) {
      type = new Type();
      type.setName(className);
      types.put(className, type);
    }
    type.addTransient(fieldName);
  }
  
  public Map getTypes() {
    return types;
  }
}
