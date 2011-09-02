/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.exceptions;

public class StatisticsGathererSessionRequiredException extends StatisticsGathererException {
  public StatisticsGathererSessionRequiredException() {
    super("A session needs to be created before performing this operation.", null);
  }
}