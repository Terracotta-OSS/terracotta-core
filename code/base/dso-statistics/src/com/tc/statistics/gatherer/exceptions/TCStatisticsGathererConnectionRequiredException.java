/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.exceptions;

public class TCStatisticsGathererConnectionRequiredException extends TCStatisticsGathererException {
  public TCStatisticsGathererConnectionRequiredException() {
    super("A connection needs to be established before performing this operation.", null);
  }
}