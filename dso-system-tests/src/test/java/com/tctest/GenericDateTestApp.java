/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.util.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class GenericDateTestApp extends GenericTransparentApp {
  private static java.util.Date referenceDate      = createDate("Feb 20, 2006");
  private static java.util.Date alternateDate      = createDate("Feb 22, 2006");
  private static Timestamp      referenceTimestamp = createTimestamp("Feb 20, 2006 03:20:20");

  public GenericDateTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, java.util.Date.class);
  }

  protected Object getTestObject(String testName) {
    List list = (List) sharedMap.get("list");
    return list.iterator();
  }

  protected void setupTestObject(String testName) {
    List list = new ArrayList();
    list.add(new java.util.Date(referenceDate.getTime()));
    list.add(new java.sql.Date(referenceDate.getTime()));
    list.add(new Time(referenceDate.getTime()));
    list.add(new Timestamp(referenceDate.getTime()));
    
    sharedMap.put("list", list);
  }

  void testModifyDate(java.util.Date date, boolean validate) {
    if (validate) {
      assertEqualDate(alternateDate, date);
    } else {
      synchronized (date) {
        date.setTime(createDate("Feb 22, 2006").getTime());
      }
    }
  }
  
  void testDateToString(java.util.Date date, boolean validate) {
    if (validate) {
      assertEqualDate(referenceDate, date);
    } else {
      synchronized (date) {
        System.out.println(date.toString());
      }
    }
  }

  // date.setYear() is a deprecated method.
  void testDateSetYear(java.util.Date date, boolean validate) {
    if (date instanceof java.sql.Time) { return; } // java.sql.Time throws IllegalArgumentException in setYear().
    
    if (validate) {
      assertEqualDate(createDate("Feb 20, 1999"), date);
    } else {
      synchronized (date) {
        date.setYear(99);
      }
    }
  }

  // date.setMonth() is a deprecated method.
  void testDateSetMonth(java.util.Date date, boolean validate) {
    if (date instanceof java.sql.Time) { return; } // java.sql.Time throws IllegalArgumentException in setMonth().
    
    if (validate) {
      assertEqualDate(createDate("Apr 20, 2006"), date);
    } else {
      synchronized (date) {
        date.setMonth(3);
      }
    }
  }

  // date.setDate() is a deprecated method.
  void testDateSetDate(java.util.Date date, boolean validate) {
    if (date instanceof java.sql.Time) { return; } // java.sql.Time throws IllegalArgumentException in setDate().
    
    if (validate) {
      assertEqualDate(createDate("Feb 03, 2006"), date);
    } else {
      synchronized (date) {
        date.setDate(3);
      }
    }
  }

  // date.setHour(), date.setMinutes(), and date.setSeconds() are deprecated methods.
  void testDateSetHourMinutesSeconds(java.util.Date date, boolean validate) {
    if (date instanceof java.sql.Date) { return; } // java.sql.Date throws IllegalArgumentException in setHours(),
                                                   // setMinutes(), and setSeconds().

    if (validate) {
      assertEqualDate(createDateTime("Feb 20, 2006 03:20:20"), date);
    } else {
      synchronized (date) {
        date.setHours(3);
        date.setMinutes(20);
        date.setSeconds(20);
      }
    }
  }
  
  void testModifyTimestamp(java.util.Date date, boolean validate) {
    if (! (date instanceof java.sql.Timestamp)) { return; }
    
    Timestamp timestamp = (Timestamp) date;
    if (validate) {
      assertEqualTimestamp(referenceTimestamp, timestamp);
    } else {
      synchronized(timestamp) {
        timestamp.setTime(referenceTimestamp.getTime());
      }
    }
  }
  
  void testTimestampSetNanos(java.util.Date date, boolean validate) {
    if (! (date instanceof java.sql.Timestamp)) { return; }
    
    Timestamp timestamp = (Timestamp) date;
    if (validate) {
      Timestamp t = new Timestamp(referenceDate.getTime());
      t.setNanos(1000000);
      assertEqualTimestamp(t, timestamp);
    } else {
      synchronized(timestamp) {
        timestamp.setNanos(1000000);
      }
    }
  }

  // Read only tests.
  void testDateReadOnlySetTime(java.util.Date date, boolean validate) {
    if (validate) {
      assertEqualDate(referenceDate, date);
    } else {
      synchronized (date) {
        try {
          date.setTime(createDate("Feb 22, 2006").getTime());
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch ( ReadOnlyException t) {
            // Expected
        }
      }
    }
  }

  void testDateReadOnlySetYear(java.util.Date date, boolean validate) {
    if (date instanceof java.sql.Time) { return; } // java.sql.Time throws IllegalArgumentException in setYear().
    
    if (validate) {
      assertEqualDate(referenceDate, date);
    } else {
      synchronized (date) {
        try {
          date.setYear(99);
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch ( ReadOnlyException t) {
            // Expected
        }
      }
    }
  }

  void testDateReadOnlySetMonth(java.util.Date date, boolean validate) {
    if (date instanceof java.sql.Time) { return; } // java.sql.Time throws IllegalArgumentException in setMonth().
    
    if (validate) {
      assertEqualDate(referenceDate, date);
    } else {
      synchronized (date) {
        try {
          date.setMonth(3);
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch ( ReadOnlyException t) {
            // Expected
        }
      }
    }
  }

  void testDateReadOnlySetDate(java.util.Date date, boolean validate) {
    if (date instanceof java.sql.Time) { return; } // java.sql.Time throws IllegalArgumentException in setDate().
    
    if (validate) {
      assertEqualDate(referenceDate, date);
    } else {
      synchronized (date) {
        try {
          date.setDate(3);
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch ( ReadOnlyException t) {
            // Expected
        }
      }
    }
  }

  void testDateReadOnlySetHoursMinutesSeconds(java.util.Date date, boolean validate) {
    if (date instanceof java.sql.Date) { return; } // java.sql.Date throws IllegalArgumentException in setHours(),
                                                   // setMinutes(), and setSeconds().
    
    if (validate) {
      assertEqualDate(referenceDate, date);
    } else {
      synchronized (date) {
        try {
          date.setHours(3);
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch ( ReadOnlyException t) {
            // Expected
        }
        try {
          date.setMinutes(20);
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch ( ReadOnlyException t) {
            // Expected
        }
        try {
          date.setSeconds(20);
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch ( ReadOnlyException t) {
            // Expected
        }
      }
    }
  }

  private static java.util.Date createDate(String dateStr) {
    try {
      return DateFormat.getDateInstance(DateFormat.MEDIUM).parse(dateStr);
    } catch (ParseException e) {
      return null;
    }
  }

  private static java.util.Date createDateTime(String dateTimeStr) {
    try {
      SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
      return df.parse(dateTimeStr);
    } catch (ParseException e) {
      return null;
    }
  }
  
  private static java.sql.Timestamp createTimestamp(String dateTimeStr) {
    java.util.Date date = createDateTime(dateTimeStr);
    Timestamp timestamp = new Timestamp(date.getTime());
    timestamp.setNanos(1000000);
    return timestamp;
  }
  
  private void assertEqualTimestamp(Timestamp expect, Timestamp actual) {
    Assert.assertNotNull(expect);
    Assert.assertNotNull(actual);
    Assert.assertTrue(expect.equals(actual));
  }

  private void assertEqualDate(java.util.Date expect, java.util.Date actual) {
    Assert.assertNotNull(expect);
    Assert.assertNotNull(actual);
    Assert.assertTrue(expect.equals(actual));
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = GenericDateTestApp.class.getName();
    config.getOrCreateSpec(testClass);
    String writeAllowedMethodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(writeAllowedMethodExpression);
    String readOnlyMethodExpression = "* " + testClass + "*.*ReadOnly*(..)";
    config.addReadAutolock(readOnlyMethodExpression);
  }

}
