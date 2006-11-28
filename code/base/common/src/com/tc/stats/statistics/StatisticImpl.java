package com.tc.stats.statistics;

import java.io.Serializable;

import javax.management.j2ee.statistics.Statistic;

/**
 * Base class for all concrete statistics.
 *
 * @see javax.management.j2ee.statistics.Statistic;
 * @see java.io.Serializable
 */

public class StatisticImpl implements Statistic, Serializable {
  private String m_name;
  private String m_unit;
  private String m_description;
  private long   m_startTime;
  private long   m_lastSampleTime;

  public StatisticImpl() {
    m_name           = "";
    m_unit           = "";
    m_description    = "";
    m_startTime      = 0;
    m_lastSampleTime = 0;
  }

  public StatisticImpl(String name, String unit, String desc, long startTime, long lastSampleTime) {
    m_name           = name;
    m_unit           = unit;
    m_description    = desc;
    m_startTime      = startTime;
    m_lastSampleTime = lastSampleTime;
  }

  public void setName(String name) {
    m_name = name;
  }

  public String getName() {
   return m_name;
  }

  public void setUnit(String unit) {
    m_unit = unit;
  }

  public String getUnit() {
   return m_unit;
  }

  public void setDescription(String description) {
    m_description = description;
  }

  public String getDescription() {
   return m_description;
  }

  public void setStartTime(long startTime) {
    m_startTime = startTime;
  }

  public long getStartTime() {
    return m_startTime;
  }

  public void setLastSampleTime(long lastSampleTime) {
    m_lastSampleTime = lastSampleTime;
  }

  public long getLastSampleTime() {
    return m_lastSampleTime;
  }

  private static final long serialVersionUID = 42;
}
