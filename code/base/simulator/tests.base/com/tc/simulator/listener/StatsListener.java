/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.simulator.listener;

public interface StatsListener {

  void sample(long sample, String desc);

}
