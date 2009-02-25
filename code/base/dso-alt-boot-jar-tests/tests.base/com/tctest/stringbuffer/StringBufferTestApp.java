/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.stringbuffer;

import org.apache.commons.lang.ClassUtils;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.GenericTransparentApp;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class StringBufferTestApp extends GenericTransparentApp {

  public StringBufferTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, StringBuddy.class);
  }

  void testOp001(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("true", buffer.toString());
      } else {
        buffer.append(true);
      }
    }
  }

  void testOp002(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("x", buffer.toString());
      } else {
        buffer.append('x');
      }
    }
  }

  void testOp003(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("tim", buffer.toString());
      } else {
        buffer.append(new char[] { 't', 'i', 'm' });
      }
    }
  }

  void testOp004(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("666", buffer.toString());
      } else {
        buffer.append(new char[] { 'a', '6', '6', '6' }, 1, 3);
      }
    }
  }

  void testOp005(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals(String.valueOf(Math.PI), buffer.toString());
      } else {
        buffer.append(Math.PI);
      }
    }
  }

  void testOp006(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals(String.valueOf((float) 2.0), buffer.toString());
      } else {
        buffer.append((float) 2.0);
      }
    }
  }

  void testOp007(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals(String.valueOf(0x7fffffff), buffer.toString());
      } else {
        buffer.append(Integer.MAX_VALUE);
      }
    }
  }

  void testOp008(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("42", buffer.toString());
      } else {
        buffer.append(42L);
      }
    }
  }

  void testOp009(StringBuddy buffer, boolean validate) {
    String testString = "fetch me blocks, o' block provider";

    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals(testString, buffer.toString());
      } else {
        // make sure this append WILL grow the buffer.
        Assert.assertTrue("buffer too large: " + buffer.capacity(), buffer.capacity() < testString.length());

        buffer.append(testString);
      }
    }
  }

  void testOp010(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("timmy", buffer.toString());
      } else {
        // make sure this append will NOT grow the buffer.
        Assert.assertTrue("buffer too small: " + buffer.capacity(), buffer.capacity() > "timmy".length());
        buffer.append("timmy");
      }
    }
  }

  void testOp011(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("timmy", buffer.toString());
      } else {
        buffer.append(new ToStringObject("timmy"));
      }
    }
  }

  void testOp012(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("0xcafebabe 0xdecafbad 0xdeadbeef", buffer.toString());
      } else {
        buffer.append(new StringBuffer("0xcafebabe 0xdecafbad 0xdeadbeef"));
      }
    }
  }

  void testOp013(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        StringBuffer defaultStringBuffer = new StringBuffer();
        Assert.eval(buffer.capacity() == defaultStringBuffer.capacity());
      } else {
        // no mutate
      }
    }
  }

  void testOp014(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals(78, buffer.capacity());
      } else {
        buffer.append("sdfjkhrkj2h34kj32h4jk2ejknb2r902jfuoinkjfb252l3u54hb2kl5hb2l35i235ou82h34ku234");
      }
    }
  }

  void testOp015(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals(69, buffer.capacity());
      } else {
        // no mutate
      }
    }
  }

  void testOp016(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals('z', buffer.charAt(7));
      } else {
        buffer.append("aaaaaa zebra");
      }
    }
  }

  void testOp017(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("toy", buffer.toString());
      } else {
        buffer.append("tommy");
        buffer.delete(2, 4);
      }
    }
  }

  void testOp018(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("joebob", buffer.toString());
      } else {
        buffer.append("joe-bob");
        buffer.deleteCharAt(3);
      }
    }
  }

  void testOp019(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals(356, buffer.capacity());
      } else {
        buffer.ensureCapacity(356);
      }
    }
  }

  void testOp020(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        char[] chars = new char[buffer.length()];
        buffer.getChars(0, buffer.length(), chars, 0);
        Assert.eval(Arrays.equals(chars, "steve is fuzzy and blue".toCharArray()));
      } else {
        buffer.append("steve is fuzzy and blue");
      }
    }
  }

  void testOp021(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals(5, buffer.indexOf("bam"));
      } else {
        buffer.append("wham bam");
      }
    }
  }

  void testOp022(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals(33, buffer.indexOf("Java", 4));
      } else {
        buffer.append("why can't I add methods to null? Java sucks");
      }
    }
  }

  void testOp023(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("Terracotta's distributed StringBuffer will not change the face of software", buffer
            .toString());
      } else {
        buffer.append("Terracotta's distributed StringBuffer will change the face of software");
        buffer.insert(43, "not ".toCharArray(), 0, 4);
      }
    }
  }

  void testOp024(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("Q. Is Steve dead sexy? true", buffer.toString());
      } else {
        buffer.append("Q. Is Steve dead sexy? ");
        buffer.insert(buffer.length(), true);
      }
    }
  }

  void testOp025(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("@55FACE", buffer.toString());
      } else {
        buffer.append("55FACE");
        buffer.insert(0, '@');
      }
    }
  }

  void testOp026(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("Dude, where's my character array?", buffer.toString());
      } else {
        buffer.append("Dude, my character array?");
        buffer.insert(6, "where's ".toCharArray());
      }
    }
  }

  void testOp027(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals(String.valueOf(Double.MAX_VALUE), buffer.toString());
      } else {
        buffer.insert(0, Double.MAX_VALUE);
      }
    }
  }

  void testOp028(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals(String.valueOf(Float.NaN), buffer.toString());
      } else {
        buffer.insert(0, Float.NaN);
      }
    }
  }

  void testOp029(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("123456789", buffer.toString());
      } else {
        buffer.insert(0, 123456789);
      }
    }
  }

  void testOp030(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("123456789123456789", buffer.toString());
      } else {
        buffer.insert(0, 123456789123456789L);
      }
    }
  }

  void testOp031(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("yer mom", buffer.toString());
      } else {
        buffer.insert(0, new ToStringObject("yer mom"));
      }
    }
  }

  void testOp032(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("chicks dig unix, but not macs", buffer.toString());
      } else {
        buffer.insert(0, "chicks dig unix, but not macs");
      }
    }
  }

  void testOp033(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals(12, buffer.lastIndexOf("ball"));
      } else {
        buffer.append("ball1 ball2 ball3");
      }
    }
  }

  void testOp034(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals(4, buffer.lastIndexOf("1", 4));
      } else {
        buffer.append("ball1 ball2 ball3");
      }
    }
  }

  void testOp035(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals(0, buffer.length());
      } else {
        // nothing
      }
    }
  }

  void testOp036(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals(77, buffer.length());
      } else {
        buffer.append("I was in a HOT TUB!  I was NORMAL!  I was ITALIAN!!  I enjoyed the EARTHQUAKE");
      }
    }
  }

  void testOp037(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("PUNK FUNK!!  DISCO DUCK!!  BIRTH CONTROL!!", buffer.toString());
      } else {
        buffer.append("PUNK ROCK!!  DISCO DUCK!!  BIRTH CONTROL!!");
        buffer.replace(5, 8, "FUN");
      }
    }
  }

  void testOp038(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("StringBuffer is da bomb", buffer.toString());
      } else {
        buffer.append("YO");
        buffer.replace(0, 2, "StringBuffer is da bomb");
      }
    }
  }

  void testOp039(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("redrum", buffer.toString());
      } else {
        buffer.append("murder");
        buffer.reverse();
      }
    }
  }

  void testOp040(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("writing StringBuffer test rates about a 0 on a scale of 10", buffer.toString());
      } else {
        buffer.append("writing StringBuffer test rates about a 8 on a scale of 10");
        buffer.setCharAt(40, '0');
      }
    }
  }

  void testOp041(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("no change", buffer.toString());
      } else {
        buffer.append("no change");
        buffer.setLength(buffer.length());
      }
    }
  }

  void testOp042(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals("changed? YES!", buffer.toString());
      } else {
        buffer.append("changed? NO");
        buffer.setLength(9);
        buffer.append("YES!");
      }
    }
  }

  void testOp043(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        // subSeqeunce and substring are the same operations
        Assert.assertEquals("almost", buffer.subSequence(0, 6));
        Assert.assertEquals("almost", buffer.substring(0, 6));

        Assert.assertEquals("tests", buffer.substring(20));
      } else {
        buffer.append("almost done writing tests");
      }
    }
  }

  void testOp044(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        StringBuffer compare = new StringBuffer();
        List vals = (List) sharedMap.get("random vals " + buffer.getClass().getName());
        for (Iterator i = vals.iterator(); i.hasNext();) {
          Integer r = (Integer) i.next();
          compare.append(r.intValue());
          compare.append(',');
        }

        Assert.assertEquals(compare.toString(), buffer.toString());
      } else {
        Random rnd = new SecureRandom();
        List vals = new ArrayList();

        final int n = 37 + rnd.nextInt(100);
        for (int i = 0; i < n; i++) {
          int r = rnd.nextInt();
          vals.add(new Integer(r));
          buffer.append(r);
          buffer.append(',');
        }

        synchronized (sharedMap) {
          sharedMap.put("random vals " + buffer.getClass().getName(), vals);
        }
      }
    }
  }

  void testOp045(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      // test that StringBuffer.getChars() to a managed array works
      String str = "these are the characters jerky";

      if (validate) {
        char[] compare = (char[]) sharedMap.get("getChars destination " + buffer.getClass());
        Assert.assertTrue(Arrays.equals(str.toCharArray(), compare));
      } else {
        buffer.append(str);
        char[] dest = new char[str.length()];
        synchronized (sharedMap) {
          sharedMap.put("getChars destination " + buffer.getClass(), dest);
        }
        buffer.getChars(0, buffer.length(), dest, 0);
      }
    }
  }

  void testOp046(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      if (validate) {
        Assert.assertEquals(String.valueOf(0x80000000), buffer.toString());
      } else {
        buffer.append(Integer.MIN_VALUE);
      }
    }
  }

  private static final String GET_CHARS_STRING = "make me slow";

  void testOp047(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      // test that StringBuffer.getChars() works when the target array is managed *and* StringBuffer is managed

      char[] target = (char[]) sharedMap.get("target array " + ClassUtils.getShortClassName(buffer.getClass()));

      if (validate) {
        Assert.assertTrue(Arrays.equals(GET_CHARS_STRING.toCharArray(), target));
      } else {
        buffer.append(GET_CHARS_STRING);
        buffer.getChars(0, buffer.length(), target, 0);
      }
    }
  }

  void testOp048(StringBuddy buffer, boolean validate) {
    synchronized (buffer) {
      // test that StringBuffer.getChars() works when the target array is managed *and* StringBuffer is NOT managed

      char[] target = (char[]) sharedMap.get("target array " + ClassUtils.getShortClassName(buffer.getClass()));

      if (validate) {
        Assert.assertTrue(Arrays.equals(GET_CHARS_STRING.toCharArray(), target));
      } else {
        StringBuffer unshared = new StringBuffer();
        unshared.append(GET_CHARS_STRING);
        synchronized (target) {
          unshared.getChars(0, unshared.length(), target, 0);
        }
      }
    }
  }

  void testStringBuilderToStringPerf(StringBuddy buddy, boolean validate) {
    if (!validate) return;
    if (!(buddy instanceof StringBuilderBuddy)) { return; }

    Random r = new Random();
    int rnd = r.nextInt();

    StringBuilder builder = new StringBuilder();

    byte[] bytes = new byte[100];
    r.nextBytes(bytes);
    for (int i = 0; i < bytes.length; i++) {
      builder.append(bytes[i]);
    }

    if (validate) {
      for (int i = 0; i < 250000; i++) {
        builder = xorChars(builder);
        String string = builder.toString();

        // not that I think it matters, but this code should defeat any runtime optimization of the above toString()
        if (string.hashCode() == rnd) {
          System.err.println();
        }
      }
    }
  }

  private static StringBuilder xorChars(StringBuilder builder) {
    char[] dest = new char[builder.length()];
    builder.getChars(0, builder.length(), dest, 0);
    for (int i = 0; i < dest.length; i++) {
      dest[i] = (char) (dest[dest.length - 1 - i] ^ dest[i]);
    }
    return new StringBuilder(new String(dest));
  }

  protected Object getTestObject(String test) {
    List l = new ArrayList();
    l.add(sharedMap.get("buffer"));
    l.add(sharedMap.get("builder"));
    return l.iterator();
  }

  protected void setupTestObject(String test) {
    final StringBuddy buffer;
    final StringBuddy builder;

    if ("Op015".equals(test)) {
      buffer = new StringBufferBuddy(new StringBuffer(69));
      builder = new StringBuilderBuddy(new StringBuilder(69));
    } else {
      buffer = new StringBufferBuddy(new StringBuffer());
      builder = new StringBuilderBuddy(new StringBuilder());
    }

    sharedMap.put("buffer", buffer);
    sharedMap.put("builder", builder);

    sharedMap.put("target array StringBuilderBuddy", new char[GET_CHARS_STRING.length()]);
    sharedMap.put("target array StringBufferBuddy", new char[GET_CHARS_STRING.length()]);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = StringBufferTestApp.class.getName();
    config.getOrCreateSpec(testClass);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    config.addIncludePattern(StringBufferBuddy.class.getName());
    config.addIncludePattern(StringBuilderBuddy.class.getName());

  }

  private static class ToStringObject {

    private final String string;

    ToStringObject(String string) {
      super();
      this.string = string;
    }

    public String toString() {
      return this.string;
    }

  }

}
