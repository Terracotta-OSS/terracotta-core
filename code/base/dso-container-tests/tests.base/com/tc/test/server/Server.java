/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.test.server;

/**
 * Represents a generic server to be run as part of a unit test.
 */
public interface Server {

  ServerResult start(ServerParameters parameters) throws Exception;

  void stop() throws Exception;
}
