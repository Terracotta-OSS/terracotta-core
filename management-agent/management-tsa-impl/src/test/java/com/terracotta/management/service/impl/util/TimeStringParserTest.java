package com.terracotta.management.service.impl.util;

import org.junit.Test;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

/**
 * @author Anthony Dahanne
 *
 * Those tests are not mocking time; so they add a 500 millis margin to the result
 * Those 500 millis could represent the difference between
 *   the test's System.currentTimeMillis() and the class under test current time in millis
 * This is precise enough for management
 *
 */
public class TimeStringParserTest {
  @Test
  public void testParseTime__day() throws Exception {
    long now = System.currentTimeMillis();
    long tenDaysinMillis =  10 * 24 * 60 * 60 * 1000L;
    long millisBetweenNowAndTenDaysAgo = now - tenDaysinMillis;

    long actual = TimeStringParser.parseTime("10d");
    assertThat(actual, greaterThanOrEqualTo(millisBetweenNowAndTenDaysAgo));
    assertThat(actual, lessThan(millisBetweenNowAndTenDaysAgo + 500));
  }

  @Test
  public void testParseTime__hour() throws Exception {
    long now = System.currentTimeMillis();
    long tenHoursinMillis =  10 * 60 * 60 * 1000L;
    long millisBetweenNowAndTenHoursAgo = now - tenHoursinMillis;

    long actual = TimeStringParser.parseTime("10h");
    assertThat(actual, greaterThanOrEqualTo(millisBetweenNowAndTenHoursAgo));
    assertThat(actual, lessThan(millisBetweenNowAndTenHoursAgo + 500));
  }

  @Test
  public void testParseTime__minute() throws Exception {
    long now = System.currentTimeMillis();
    long tenMinutesinMillis =  10 * 60 * 1000L;
    long millisBetweenNowAndTenMinutessAgo = now - tenMinutesinMillis;

    long actual = TimeStringParser.parseTime("10m");
    assertThat(actual, greaterThanOrEqualTo(millisBetweenNowAndTenMinutessAgo));
    assertThat(actual, lessThan(millisBetweenNowAndTenMinutessAgo + 500));
  }

  @Test
  public void testParseTime_second() throws Exception {
    long now = System.currentTimeMillis();
    long tenSecondsinMillis =  10 * 1000L;
    long millisBetweenNowAndTenSecondsAgo = now - tenSecondsinMillis;

    long actual = TimeStringParser.parseTime("10s");
    assertThat(actual, greaterThanOrEqualTo(millisBetweenNowAndTenSecondsAgo));
    assertThat(actual, lessThan(millisBetweenNowAndTenSecondsAgo + 500));
  }

  @Test
  public void testParseTime() throws Exception {
    long now = System.currentTimeMillis();

    long actual = TimeStringParser.parseTime(Long.toString(now));
    assertThat(actual, greaterThanOrEqualTo(now));
    assertThat(actual, lessThan(now + 500));
  }


}
