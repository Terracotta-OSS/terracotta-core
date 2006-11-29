/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.aj;

public aspect GetFieldAspect {

  pointcut onGetValue() : execution(* InstrumentedBean.getValue(..));
  
  pointcut onGetValueField() : get(* InstrumentedBean.value);

  Object around() : onGetValue() {
    System.err.println("Around getValue()");
    return proceed();
  }
  
  String around() : onGetValueField() {
    String value = proceed();
    System.err.println("Around get field value: "+ value);
    return value;
  }
  
}
