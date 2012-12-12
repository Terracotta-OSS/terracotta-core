/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.OperatorEventEntity;

import java.util.Collection;
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
public interface OperatorEventsService {

  /**
   * Get the operator events of the specified servers
   * @param serverNames A set of server names, null meaning all of them.
   * @param sinceWhen A string describing a timestamp that will be parsed.
   * @param filterOutRead true if the read operator events should be filter out, false otherwise.
   * @return a collection of operator events
   * @throws org.terracotta.management.ServiceExecutionException
   */
  Collection<OperatorEventEntity> getOperatorEvents(Set<String> serverNames, String sinceWhen, boolean filterOutRead) throws ServiceExecutionException;

  /**
   * Mark an operator even as read or unread.
   * @param operatorEventEntity the operator event to mark.
   * @param read true if the operator even should be marked as read, false otherwise.
   * @throws ServiceExecutionException
   */
  void markOperatorEvent(OperatorEventEntity operatorEventEntity, boolean read) throws ServiceExecutionException;

}
