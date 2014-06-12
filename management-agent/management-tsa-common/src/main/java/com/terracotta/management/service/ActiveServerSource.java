/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import java.util.List;

/**
 * @author Ludovic Orban
 */
public interface ActiveServerSource {

  boolean isCurrentServerActive();

  List<String> getActiveL2Urls() throws ServiceExecutionException;

}
