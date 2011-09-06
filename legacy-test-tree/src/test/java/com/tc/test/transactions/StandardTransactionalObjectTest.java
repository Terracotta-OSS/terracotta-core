/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.transactions;

import com.tc.test.TCTestCase;
import com.tc.util.TCAssertionError;

/**
 * Unit test for {@link StandardTransactionalObject}.
 */
public class StandardTransactionalObjectTest extends TCTestCase {

  private TransactionalObject trans;

  @Override
  public void setUp() throws Exception {
    this.trans = new StandardTransactionalObject("test-object", "foo");
  }

  /**
   * This tests for a very specific case, where we could end up DGCing old data that we might need later &mdash; this is
   * what the DGC slop in {@link StandardTransactionalObject} is for.
   */
  public void testMixedSpecifiedAndUnspecifiedTimes() throws Exception {
    TransactionalObject.Context write6615 = this.trans.startWrite("6615");
    this.trans.endWrite(write6615);

    Thread.sleep(100);

    TransactionalObject.Context write6616 = this.trans.startWrite("6616");

    Thread.sleep(100);

    long readTime = System.currentTimeMillis();

    Thread.sleep(100);

    this.trans.endWrite(write6616);

    Thread.sleep(100);

    TransactionalObject.Context read = this.trans.startRead();
    this.trans.endRead(read, "6616");

    read = this.trans.startRead(readTime);
    this.trans.endRead(read, "6615");
  }

  public void xtestSimpleWrites() throws Exception {
    checkValues(new String[] { "foo" }, new String[] { "bar", "baz", "quux" });

    StandardTransactionalObject.Context context1 = this.trans.startWrite("bar");
    checkValues(new String[] { "foo", "bar" }, new String[] { "baz", "quux" });

    this.trans.endWrite(context1);
    checkValues(new String[] { "bar" }, new String[] { "foo", "baz", "quux" });

    context1 = this.trans.startWrite("foo");
    StandardTransactionalObject.Context context2 = this.trans.startWrite("baz");

    checkValues(new String[] { "foo", "bar", "baz" }, new String[] { "quux" });
    this.trans.endWrite(context1);
    checkValues(new String[] { "foo", "baz" }, new String[] { "bar", "quux" });
    this.trans.endWrite(context2);
    checkValues(new String[] { "foo", "baz" }, new String[] { "bar", "quux" });
  }

  public void xtestInterlacedReads() {
    this.trans = new StandardTransactionalObject("test-object", 0, "foo", 1000);

    StandardTransactionalObject.Context readContext1 = this.trans.startRead(1020);
    StandardTransactionalObject.Context readContext1b = this.trans.startRead(1020);
    StandardTransactionalObject.Context readContext1c = this.trans.startRead(1020);
    StandardTransactionalObject.Context writeContext1 = this.trans.startWrite("bar", 1040);
    StandardTransactionalObject.Context readContext2 = this.trans.startRead(1060);
    StandardTransactionalObject.Context readContext2b = this.trans.startRead(1060);
    StandardTransactionalObject.Context readContext2c = this.trans.startRead(1060);
    StandardTransactionalObject.Context writeContext2 = this.trans.startWrite("baz", 1080);
    StandardTransactionalObject.Context readContext3 = this.trans.startRead(1100);
    StandardTransactionalObject.Context readContext3b = this.trans.startRead(1100);
    StandardTransactionalObject.Context readContext3c = this.trans.startRead(1100);

    checkValues(readContext1, 1020, new String[] { "foo" }, new String[] { "bar", "baz" });
    checkValues(readContext1, 1030, new String[] { "foo" }, new String[] { "bar", "baz" });
    checkValues(readContext1, 1039, new String[] { "foo" }, new String[] { "bar", "baz" });
    checkValues(readContext1, 1040, new String[] { "foo", "bar" }, new String[] { "baz" });
    checkValues(readContext1, 1079, new String[] { "foo", "bar" }, new String[] { "baz" });
    checkValues(readContext1, 1080, new String[] { "foo", "bar", "baz" }, new String[] {});
    checkValues(readContext1, 1120, new String[] { "foo", "bar", "baz" }, new String[] {});

    checkValues(readContext2, 1060, new String[] { "foo", "bar" }, new String[] { "baz" });
    checkValues(readContext2, 1079, new String[] { "foo", "bar" }, new String[] { "baz" });
    checkValues(readContext2, 1080, new String[] { "foo", "bar", "baz" }, new String[] {});
    checkValues(readContext2, 1120, new String[] { "foo", "bar", "baz" }, new String[] {});

    checkValues(readContext3, 1100, new String[] { "foo", "bar", "baz" }, new String[] {});

    this.trans.endWrite(writeContext1, 1140);

    checkValues(readContext1b, 1160, new String[] { "foo", "bar", "baz" }, new String[] {});
    checkValues(readContext2b, 1160, new String[] { "foo", "bar", "baz" }, new String[] {});
    checkValues(readContext3b, 1160, new String[] { "foo", "bar", "baz" }, new String[] {});

    this.trans.endWrite(writeContext2, 1180);

    checkValues(readContext1c, 1200, new String[] { "foo", "bar", "baz" }, new String[] {});
    checkValues(readContext2c, 1200, new String[] { "foo", "bar", "baz" }, new String[] {});
    checkValues(readContext3c, 1200, new String[] { "foo", "bar", "baz" }, new String[] {});

    StandardTransactionalObject.Context readContext4 = this.trans.startRead(1180);

    checkValues(readContext4, 1180, new String[] { "bar", "baz" }, new String[] { "foo" });
  }

  public void xtestSlop() throws Exception {
    this.trans = new StandardTransactionalObject("test-object", 100, "foo", 1000);

    StandardTransactionalObject.Context context1 = this.trans.startWrite("bar", 1050);
    checkValues(1050, new String[] { "foo", "bar" }, new String[] { "baz", "quux" });
    this.trans.endWrite(context1, 1070);

    checkValues(1070, new String[] { "foo", "bar" }, new String[] { "baz", "quux" });
    checkValues(1100, new String[] { "foo", "bar" }, new String[] { "baz", "quux" });
    checkValues(1169, new String[] { "foo", "bar" }, new String[] { "baz", "quux" });
    checkValues(1170, new String[] { "foo", "bar" }, new String[] { "baz", "quux" });
    checkValues(1171, new String[] { "bar" }, new String[] { "foo", "baz", "quux" });
    checkValues(1500, new String[] { "bar" }, new String[] { "foo", "baz", "quux" });

    context1 = this.trans.startWrite("baz", 2000);
    checkValues(2000, new String[] { "bar", "baz" }, new String[] { "foo", "quux" });
    checkValues(2050, new String[] { "bar", "baz" }, new String[] { "foo", "quux" });
    checkValues(2150, new String[] { "bar", "baz" }, new String[] { "foo", "quux" });
    StandardTransactionalObject.Context context2 = this.trans.startWrite("quux", 2200);

    checkValues(2200, new String[] { "bar", "baz", "quux" }, new String[] { "foo" });
    checkValues(2300, new String[] { "bar", "baz", "quux" }, new String[] { "foo" });
    checkValues(2400, new String[] { "bar", "baz", "quux" }, new String[] { "foo" });

    this.trans.endWrite(context2, 2500);
    checkValues(2500, new String[] { "bar", "baz", "quux" }, new String[] { "foo" });
    checkValues(2599, new String[] { "bar", "baz", "quux" }, new String[] { "foo" });
    checkValues(2600, new String[] { "bar", "baz", "quux" }, new String[] { "foo" });
    checkValues(2601, new String[] { "baz", "quux" }, new String[] { "foo", "bar" });

    this.trans.endWrite(context1, 2800);
    checkValues(2800, new String[] { "baz", "quux" }, new String[] { "foo", "bar" });
    checkValues(2801, new String[] { "baz", "quux" }, new String[] { "foo", "bar" });
    checkValues(2900, new String[] { "baz", "quux" }, new String[] { "foo", "bar" });

    // This is important: because we can't actually know which write happened first, it needs to keep returning both
    // values.
    checkValues(2901, new String[] { "baz", "quux" }, new String[] { "foo", "bar" });
    checkValues(5000, new String[] { "baz", "quux" }, new String[] { "foo", "bar" });
  }

  private void checkValues(String[] accepted, String[] notAccepted) {
    checkValues(System.currentTimeMillis(), accepted, notAccepted);
  }

  private void checkValues(long when, String[] accepted, String[] notAccepted) {
    checkValues(this.trans.startRead(when), when, accepted, notAccepted);
  }

  private void checkValues(StandardTransactionalObject.Context context, long when, String[] accepted,
                           String[] notAccepted) {
    for (int i = 0; i < accepted.length; ++i) {
      this.trans.endRead(context, accepted[i], when);
    }

    for (int i = 0; i < notAccepted.length; ++i) {
      try {
        this.trans.endRead(context, notAccepted[i], when);
        fail("Didn't get TCAE on endRead with bad value");
      } catch (TCAssertionError tcae) {
        // ok
      }
    }

  }

}
