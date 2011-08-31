/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.statistics;

/**
 * This class allows an SRA instance to initialized after its registration.
 * <p>
 * This is very useful when the SRA class needs to be loaded by a particular class loader, for example the application
 * class loader in TIMs.
 * <p>
 * It's up to the application code to properly implement the logic to set a delegate SRA instance at the right time and
 * in the right context. Until that is done, the SRA will always return empty data.
 * <p>
 * To register the delegate, you'll want to use the {@code
 * com.tc.object.bytecode.Manager#getStatisticRetrievalActionInstance} to get access to the instance of the {@code
 * LazilyInitializedSRA} that you want to get access to.
 */
public class LazilyInitializedSRA implements StatisticRetrievalAction {

  private final String                      name;
  private final StatisticType               type;

  private volatile StatisticRetrievalAction delegate;

  /**
   * Creates a new instance for a particular SRA name and type.
   */
  public LazilyInitializedSRA(final String name, final StatisticType type) {
    this.name = name;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public StatisticType getType() {
    return type;
  }

  /**
   * Retrieves the delegate SRA that has been registered with this lazy SRA.
   */
  public StatisticRetrievalAction getDelegate() {
    return this.delegate;
  }

  /**
   * Sets the delegate SRA instance that will be used.
   *
   * @param delegate the delegate SRA instance
   */
  public void setDelegate(final StatisticRetrievalAction delegate) {
    this.delegate = delegate;
  }

  public StatisticData[] retrieveStatisticData() {
    if (null == delegate) { return EMPTY_STATISTIC_DATA; }

    return delegate.retrieveStatisticData();
  }

}
