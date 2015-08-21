/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats;

public interface Monitorable {
  
  public void enableStatsCollection(boolean enable);
  
  public boolean isStatsCollectionEnabled();
  
  /*
   * @param - frequency is the millis since the last call.
   */
  public Stats getStats(long frequency);
  
  /*
   * @param - frequency is the millis since the last call.
   */
  public Stats getStatsAndReset(long frequency);
  
  public void resetStats();
}
