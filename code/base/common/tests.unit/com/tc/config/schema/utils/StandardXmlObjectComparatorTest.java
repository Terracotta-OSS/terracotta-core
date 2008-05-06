/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.utils;

import com.tc.test.TCTestCase;
import com.terracottatech.configTest.TestRootDocument;
import com.terracottatech.configTest.TestRootDocument.TestRoot;

/**
 * Unit test for {@link StandardXmlObjectComparator}.
 */
public class StandardXmlObjectComparatorTest extends TCTestCase {

  private static final String ORIGINAL = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
  "\n" +
  "<t:test-root xmlns:t=\"http://www.terracottatech.com/config-test\">\n" +
  "  <element attr4=\"funk\">\n" + 
  "    <inner-1>foobar</inner-1>\n" +
  "    <inner-2>147</inner-2>\n" +
  "    <inner-4>\n" +
  "      <complex-1>baz</complex-1>\n" +
  "    </inner-4>\n" +
  "  </element>\n" +
  "</t:test-root>\n";
  
  private static final String EQUIVALENT_1 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
  "\n" +
  "<t:test-root xmlns:t=\"http://www.terracottatech.com/config-test\">\n" +
  "  <element attr4=\"funk\">\n" + 
  "    <inner-1>foobar</inner-1>\n" +
  "    <inner-2>  0147  </inner-2>\n" +
  "    <inner-4>\n" +
  "      <complex-1>baz</complex-1>\n" +
  "    </inner-4>\n" +
  "  </element>\n" +
  "</t:test-root>\n";
  
  private static final String DIFFERENT_1 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
  "\n" +
  "<t:test-root xmlns:t=\"http://www.terracottatech.com/config-test\">\n" +
  "  <element attr4=\"Funk\">\n" + 
  "    <inner-1>foobar</inner-1>\n" +
  "    <inner-2>147</inner-2>\n" +
  "    <inner-4>\n" +
  "      <complex-1>baz</complex-1>\n" +
  "    </inner-4>\n" +
  "  </element>\n" +
  "</t:test-root>\n";
  
  private static final String DIFFERENT_10 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
  "\n" +
  "<t:test-root xmlns:t=\"http://www.terracottatech.com/config-test\">\n" +
  "  <element attr4=\" funk \">\n" + 
  "    <inner-1>foobar</inner-1>\n" +
  "    <inner-2>147</inner-2>\n" +
  "    <inner-4>\n" +
  "      <complex-1>baz</complex-1>\n" +
  "    </inner-4>\n" +
  "  </element>\n" +
  "</t:test-root>\n";
  
  private static final String DIFFERENT_2 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
  "\n" +
  "<t:test-root xmlns:t=\"http://www.terracottatech.com/config-test\">\n" +
  "  <element attr4=\"funk\">\n" + 
  "    <inner-1>foobar </inner-1>\n" +
  "    <inner-2>147</inner-2>\n" +
  "    <inner-4>\n" +
  "      <complex-1>baz</complex-1>\n" +
  "    </inner-4>\n" +
  "  </element>\n" +
  "</t:test-root>\n";
  
  private static final String DIFFERENT_3 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
  "\n" +
  "<t:test-root xmlns:t=\"http://www.terracottatech.com/config-test\">\n" +
  "  <element attr4=\"funk\">\n" + 
  "    <inner-1>foobar</inner-1>\n" +
  "    <inner-2>148</inner-2>\n" +
  "    <inner-4>\n" +
  "      <complex-1>baz</complex-1>\n" +
  "    </inner-4>\n" +
  "  </element>\n" +
  "</t:test-root>\n";
  
  private static final String DIFFERENT_4 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
  "\n" +
  "<t:test-root xmlns:t=\"http://www.terracottatech.com/config-test\">\n" +
  "  <element attr4=\"funk\">\n" + 
  "    <inner-1>foobar</inner-1>\n" +
  "    <inner-2>147</inner-2>\n" +
  "  </element>\n" +
  "</t:test-root>\n";
  
  private static final String DIFFERENT_5 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
  "\n" +
  "<t:test-root xmlns:t=\"http://www.terracottatech.com/config-test\">\n" +
  "  <element attr4=\"funk\">\n" + 
  "    <inner-1>foobar</inner-1>\n" +
  "    <inner-2>147</inner-2>\n" +
  "    <inner-4>\n" +
  "      <complex-1>bar</complex-1>\n" +
  "    </inner-4>\n" +
  "  </element>\n" +
  "</t:test-root>\n";
  
  private static final String DIFFERENT_6 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
  "\n" +
  "<t:test-root xmlns:t=\"http://www.terracottatech.com/config-test\">\n" +
  "  <element attr4=\"funk\">\n" + 
  "    <inner-1>foobar</inner-1>\n" +
  "    <inner-2>147</inner-2>\n" +
  "    <inner-4>\n" +
  "      <complex-1>Baz</complex-1>\n" +
  "    </inner-4>\n" +
  "  </element>\n" +
  "</t:test-root>\n";
  
  private static final String DIFFERENT_7 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
  "\n" +
  "<t:test-root xmlns:t=\"http://www.terracottatech.com/config-test\">\n" +
  "  <element attr4=\"funk\">\n" + 
  "    <inner-1>foobar</inner-1>\n" +
  "    <inner-2>147</inner-2>\n" +
  "    <inner-4>\n" +
  "      <complex-1>  baz </complex-1>\n" +
  "    </inner-4>\n" +
  "  </element>\n" +
  "</t:test-root>\n";
  
  private static final String DIFFERENT_8 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
  "\n" +
  "<t:test-root xmlns:t=\"http://www.terracottatech.com/config-test\">\n" +
  "  <element attr4=\"funk\">\n" + 
  "    <inner-1>foobar</inner-1>\n" +
  "    <inner-2>147</inner-2>\n" +
  "    <inner-4>\n" +
  "      <complex-1>baz</complex-1>\n" +
  "      <complex-2 />\n" +
  "    </inner-4>\n" +
  "  </element>\n" +
  "</t:test-root>\n";
  
  private static final String DIFFERENT_9 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
  "\n" +
  "<t:test-root xmlns:t=\"http://www.terracottatech.com/config-test\">\n" +
  "  <element attr4=\"funk\">\n" + 
  "    <inner-1>foobar</inner-1>\n" +
  "    <inner-2>147</inner-2>\n" +
  "    <inner-4>\n" +
  "      <complex-1>baz</complex-1>\n" +
  "      <complex-2>4</complex-2>\n" +
  "    </inner-4>\n" +
  "  </element>\n" +
  "</t:test-root>\n";

  private StandardXmlObjectComparator comparator;

  public void setUp() throws Exception {
    comparator = new StandardXmlObjectComparator();
  }

  public void testEquivalent() throws Exception {
    checkEquals(ORIGINAL, ORIGINAL);
    checkEquals(ORIGINAL, EQUIVALENT_1);
  }
  
  public void testNotEquivalent() throws Exception {
    checkNotEquals(ORIGINAL, DIFFERENT_1, "/element/attr4");
    checkNotEquals(ORIGINAL, DIFFERENT_2, "/element/inner1");
    checkNotEquals(ORIGINAL, DIFFERENT_3, "/element/inner2");
    checkNotEquals(ORIGINAL, DIFFERENT_4, "/element/inner4");
    checkNotEquals(ORIGINAL, DIFFERENT_5, "/element/inner4/complex1");
    checkNotEquals(ORIGINAL, DIFFERENT_6, "/element/inner4/complex1");
    checkNotEquals(ORIGINAL, DIFFERENT_7, "/element/inner4/complex1");
    checkNotEquals(ORIGINAL, DIFFERENT_8, "/element/inner4/complex2");
    checkNotEquals(ORIGINAL, DIFFERENT_9, "/element/inner4/complex2");
    checkNotEquals(ORIGINAL, DIFFERENT_10, "/element/attr4");
  }
  
  private void checkNotEquals(String one, String two, String where) throws Exception {
    check(one, two, where);
  }
  
  private void checkEquals(String one, String two) throws Exception {
    check(one, two, null);
  }
  
  private void check(String one, String two, String where) throws Exception {
    TestRootDocument docOne = TestRootDocument.Factory.parse(one);
    TestRootDocument docTwo = TestRootDocument.Factory.parse(two);
    
    TestRoot rootOne = docOne.getTestRoot();
    TestRoot rootTwo = docTwo.getTestRoot();
    
    try {
      this.comparator.checkEquals(rootOne, rootTwo);
      if (where != null) fail("Should've gotten exception, but didn't.");
    } catch (NotEqualException nee) {
      if (where == null) throw nee;
      assertContains(where, nee.getMessage());
    }
    
    assertEquals(where == null, this.comparator.equals(rootOne, rootTwo));
  }

}
