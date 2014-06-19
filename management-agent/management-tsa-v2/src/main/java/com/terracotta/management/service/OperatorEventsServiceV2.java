/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.ResponseEntityV2;

import com.terracotta.management.resource.OperatorEventEntityV2;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * An interface for service implementations providing TSA operator events querying facilities.
 * <p/>
 * The timestamp string describes a time since <i>now</i>.
 * The grammar for the timestamp string is as follows:
 * <pre>&lt;numeric value&gt;&lt;unit&gt;</pre>
 * Unit must be in this list:
 * <ul>
 * <li><b>d</b> for days</li>
 * <li><b>h</b> for hours</li>
 * <li><b>m</b> for minutes</li>
 * <li><b>s</b> for seconds</li>
 * </ul>
 * <p/>
 * For instance, these strings are valid:
 * <ul>
 * <li><b>2d</b> means in the last 2 days</li>
 * <li><b>24h</b> means in the last 24 hours</li>
 * <li><b>1m</b> means in the last minute</li>
 * <li><b>10s</b> means in the last 10 seconds</li>
 * </ul>
 *
 * @author Ludovic Orban
 */
public interface OperatorEventsServiceV2 {

  /**
   * Get the operator events of the specified servers
   * @param serverNames A set of server names, null meaning all of them.
   * @param sinceWhen A string describing a timestamp that will be parsed to filter out events that happened before or at that time, null meaning no such filtering.
   * @param eventTypes A string describing comma-separated event types to filter out events of different types, null meaning no such filtering.
   * @param eventLevels A string describing comma-separated event levels to filter out events of different log levels, null meaning no such filtering.
   * @param filterOutRead true if the read operator events should be filter out, false otherwise.
   * @return a collection of operator events
   * @throws org.terracotta.management.ServiceExecutionException
   */
  ResponseEntityV2<OperatorEventEntityV2> getOperatorEvents(Set<String> serverNames, String sinceWhen, String eventTypes, String eventLevels, boolean filterOutRead) throws ServiceExecutionException;

  /**
   * Mark operator events as read or unread.
   * @param operatorEventEntities the operator events to mark.
   * @param read true if the operator even should be marked as read, false otherwise.
   * @return true if the event was found and marked, false otherwise.
   * @throws ServiceExecutionException
   */
  boolean markOperatorEvents(Collection<OperatorEventEntityV2> operatorEventEntities, boolean read) throws ServiceExecutionException;

  /**
   * Returns the number of unread operator events.
   * @throws ServiceExecutionException
   * @param serverNames A set of server names, null meaning all of them.
   */
  Map<String, Integer> getUnreadCount(Set<String> serverNames) throws ServiceExecutionException;
}
