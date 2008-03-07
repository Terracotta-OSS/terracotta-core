/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.exceptions;

public class TCStatisticsGathererAlreadyConnectedException extends TCStatisticsGathererException {
  public TCStatisticsGathererAlreadyConnectedException() {
    super("A connection has already been established beforehand.", null);
  }
}