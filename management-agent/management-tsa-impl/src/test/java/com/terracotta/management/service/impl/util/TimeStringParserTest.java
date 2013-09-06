package com.terracotta.management.service.impl.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
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

    assertThat(TimeStringParser.parseTime("10d"), is(both(greaterThanOrEqualTo(millisBetweenNowAndTenDaysAgo)).and(lessThan(millisBetweenNowAndTenDaysAgo + 500))));
  }

  @Test
  public void testParseTime__hour() throws Exception {
    long now = System.currentTimeMillis();
    long tenHoursinMillis =  10 * 60 * 60 * 1000L;
    long millisBetweenNowAndTenHoursAgo = now - tenHoursinMillis;

    assertThat(TimeStringParser.parseTime("10h"), is(both(greaterThanOrEqualTo(millisBetweenNowAndTenHoursAgo)).and(lessThan(millisBetweenNowAndTenHoursAgo + 500))));
  }

  @Test
  public void testParseTime__minute() throws Exception {
    long now = System.currentTimeMillis();
    long tenMinutesinMillis =  10 * 60 * 1000L;
    long millisBetweenNowAndTenMinutessAgo = now - tenMinutesinMillis;

    assertThat(TimeStringParser.parseTime("10m"), is(both(greaterThanOrEqualTo(millisBetweenNowAndTenMinutessAgo)).and(lessThan(millisBetweenNowAndTenMinutessAgo + 500))));
  }

  @Test
  public void testParseTime_second() throws Exception {
    long now = System.currentTimeMillis();
    long tenSecondsinMillis =  10 * 1000L;
    long millisBetweenNowAndTenSecondsAgo = now - tenSecondsinMillis;

    assertThat(TimeStringParser.parseTime("10s"), is(both(greaterThanOrEqualTo(millisBetweenNowAndTenSecondsAgo)).and(lessThan(millisBetweenNowAndTenSecondsAgo + 500))));
  }

  @Test
  public void testParseTime() throws Exception {
    long now = System.currentTimeMillis();

    assertThat(TimeStringParser.parseTime(Long.toString(now)), is(both(greaterThanOrEqualTo(now)).and(lessThan(now + 500))));
  }


}
