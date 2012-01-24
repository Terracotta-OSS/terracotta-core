/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

/**
 * This interface has to be implemented to retrieve statistics data from system.
 * {@code StatisticRetrievalAction} is abbreviated as SRA throughout the
 * statistics infrastructure.
 *
 * The accent is really situated on the 'retrieval' part. Usually, a
 * {@code StatisticRetrievalAction} implementation will interact with
 * another part of the system that is responsible for measuring and sampling
 * the data itself.
 *
 * SRA classes are instantiated only once within the agent that they
 * belong to. The instances are shared amongst statistics capture sessions. Therefore,
 * SRA classes should not have any non-final instance state.
 */
public interface StatisticRetrievalAction {
  /**
   * Convenience field that allows SRAs to return empty data.
   */
  public final static StatisticData[] EMPTY_STATISTIC_DATA = new StatisticData[0];

  /**
   * Retrieves statistics data from the system and formats it as an array of
   * {@code StatisticData} instances.
   *
   * Instead of returning {@code null} when data is not available,
   * it's recommended to return {@link #EMPTY_STATISTIC_DATA}.
   *
   * All expected exceptions that occur during data retrieval should
   * be handled within this method and shouldn't bubble up. For instance of
   * infinite or NaN values are present as data values. In those situations
   * one can either create empty data values or a return an empty array. This
   * depends on what is appropriate for this particular SRA implementation.
   *
   * @return the retrieved statistic data
   */
  public StatisticData[] retrieveStatisticData();

  /**
   * The name that uniquely identifies this SRA.
   *
   * This name is used when SRAs are listed by a statistics agent and when they are
   * activated/deactivated. This is independent of the name that is used
   * within the {@code StatisticData} instances that are returned from
   * {@link #retrieveStatisticData}. It is however recommended for the SRA
   * name to be the prefix of the name within {@code StatisticData}. Note
   * that it's recommended to have this name available as a
   * {@code public static final} class field to allow other parts of the code to
   * refer to this name without having to hard-code strings.
   *
   * @return the name of this SRA
   */
  public String getName();

  /**
   * Returns the type of this SRA class.
   *
   * @return the type instance
   */
  public StatisticType getType();
}