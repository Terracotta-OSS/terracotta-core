/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.LogEntity;

import java.util.Collection;
import java.util.Set;

/**
 * An interface for service implementations providing TSA logs querying facilities.
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
public interface LogsService {

  /**
   * Get all the logs of the specified servers
   * @param serverNames A set of server names, null meaning all of them.
   * @return a collection of logs
   * @throws org.terracotta.management.ServiceExecutionException
   */
  Collection<LogEntity> getLogs(Set<String> serverNames) throws ServiceExecutionException;

  /**
   * Get the logs of the specified servers
   * @param serverNames A set of server names, null meaning all of them.
   * @param sinceWhen A timestamp used to filter out logs, only the ones newer than or at the
   *                  specified timestamp will be returned.
   * @return a collection of logs
   * @throws org.terracotta.management.ServiceExecutionException
   */
  Collection<LogEntity> getLogs(Set<String> serverNames, long sinceWhen) throws ServiceExecutionException;

  /**
   * Get the logs of the specified servers
   * @param serverNames A set of server names, null meaning all of them.
   * @param sinceWhen A string describing a timestamp that will be parsed.
   * @return a collection of logs
   * @throws org.terracotta.management.ServiceExecutionException
   */
  Collection<LogEntity> getLogs(Set<String> serverNames, String sinceWhen) throws ServiceExecutionException;


}
