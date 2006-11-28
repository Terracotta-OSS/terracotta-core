package com.tc.async.api;

/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
/**
 * @author steve Represents
 */
public interface Stage {

  public final static int NO_MAX_QUEUE_SIZE = -1;

  public void destroy();

  public Sink getSink();

  public String getName();

  public void start(ConfigurationContext context);
  
  public void pause();
  
  public void unpause();
}