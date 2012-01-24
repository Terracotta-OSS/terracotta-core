/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class indicates how a {@link StatisticRetrievalAction} should be automatically captured during a capturing
 * session.
 */
public class StatisticType {
  private final static Map<String, StatisticType> TYPES     = new ConcurrentHashMap<String, StatisticType>();

  /**
   * Statistic will be automatically captured at the beginning of a capturing session.
   */
  public final static StatisticType               STARTUP   = new StatisticType("STARTUP");

  /**
   * Statistics will be automatically captured during the capturing session, based on the frequency that has been setup
   * on the configuration.
   */
  public final static StatisticType               SNAPSHOT  = new StatisticType("SNAPSHOT");

  /**
   * Statistics of this type will not captured automatically. They can however be captured on an as-needed basis through
   * API calls.
   */
  public final static StatisticType               TRIGGERED = new StatisticType("TRIGGERED");

  private final String                            identifier;

  private StatisticType(final String identifier) {
    if (identifier == null) { throw new AssertionError("identifier is null"); }
    this.identifier = identifier;
    TYPES.put(identifier, this);
  }

  public static StatisticType getType(final String identifier) {
    if (null == identifier) { return null; }
    return TYPES.get(identifier.toUpperCase());
  }

  public static Collection getAllTypes() {
    return Collections.unmodifiableCollection(TYPES.values());
  }

  @Override
  public String toString() {
    return identifier;
  }

  @Override
  public int hashCode() {
    return identifier.hashCode();
  }

  @Override
  public boolean equals(final Object object) {
    if (null == object) { return false; }

    return (object instanceof StatisticType) && ((StatisticType) object).identifier.equals(identifier);
  }
}
