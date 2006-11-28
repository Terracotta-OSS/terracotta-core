/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */

package com.tctest.spring.bean;

public interface CounterSaver {

  void saveCounter();
  
  int getSavedCounter();
  
}
