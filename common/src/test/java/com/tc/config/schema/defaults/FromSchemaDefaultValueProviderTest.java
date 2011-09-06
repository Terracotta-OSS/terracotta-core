/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.defaults;

import org.apache.xmlbeans.XmlBoolean;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlInteger;
import org.apache.xmlbeans.XmlString;

import com.tc.test.TCTestCase;
import com.terracottatech.configTest.TestRootDocument.TestRoot;

/**
 * Unit test for {@link SchemaDefaultValueProvider}.
 */
public class FromSchemaDefaultValueProviderTest extends TCTestCase {

  private SchemaDefaultValueProvider provider;

  public void setUp() throws Exception {
    this.provider = new SchemaDefaultValueProvider();
  }

  private void checkHasDefault(String xpath) throws Exception {
    assertTrue(this.provider.possibleForXPathToHaveDefault(xpath));
    assertTrue(this.provider.hasDefault(TestRoot.type, xpath));
  }

  public void testDefaultFor() throws Exception {
    checkHasDefault("element/inner-3");
    assertEquals(19235, ((XmlInteger) this.provider.defaultFor(TestRoot.type, "element/inner-3")).getBigIntegerValue()
        .intValue());

    checkHasDefault("element/inner-4/complex-2");
    assertEquals(423456, ((XmlInteger) this.provider.defaultFor(TestRoot.type, "element/inner-4/complex-2"))
        .getBigIntegerValue().intValue());

    checkHasDefault("element/inner-4/complex-4");
    assertEquals(true, ((XmlBoolean) this.provider.defaultFor(TestRoot.type, "element/inner-4/complex-4"))
        .getBooleanValue());

    checkHasDefault("element/inner-4/complex-5");
    assertEquals("FUNKiness", ((XmlString) this.provider.defaultFor(TestRoot.type, "element/inner-4/complex-5"))
        .getStringValue());

    checkHasDefault("element/@attr1");
    assertEquals("funk", ((XmlString) this.provider.defaultFor(TestRoot.type, "element/@attr1")).getStringValue());

    checkHasDefault("element/@attr2");
    assertEquals(1795, ((XmlInteger) this.provider.defaultFor(TestRoot.type, "element/@attr2")).getBigIntegerValue()
        .intValue());
  }

  public void testInvalidXPath() throws Exception {
    try {
      this.provider.defaultFor(TestRoot.type, "element/inner-4/--foo");
      fail("Didn't get XMLE on attempt at attribute");
    } catch (XmlException xmle) {
      // ok
    }
  }

  public void testPathIntoNowhere() throws Exception {
    try {
      this.provider.defaultFor(TestRoot.type, "element/inner-4/complex-2/foo");
      fail("Didn't get XMLE on path into nowhere");
    } catch (XmlException xmle) {
      // ok
    }
  }

  public void testNonexistentAttribute() throws Exception {
    try {
      this.provider.defaultFor(TestRoot.type, "element/@attrnonexistent");
      fail("Didn't get XMLE on path into nowhere");
    } catch (XmlException xmle) {
      // ok
    }
  }

  public void testAttributeOfElementWithoutAttributes() throws Exception {
    try {
      this.provider.defaultFor(TestRoot.type, "element/inner-4/@attr");
      fail("Didn't get XMLE on path into nowhere");
    } catch (XmlException xmle) {
      // ok
    }
  }

  public void testAttributePathIntoNowhere() throws Exception {
    try {
      this.provider.defaultFor(TestRoot.type, "element/inner-4/complex-2/@foo");
      fail("Didn't get XMLE on path into nowhere");
    } catch (XmlException xmle) {
      // ok
    }
  }

  public void testIncorrectPath() throws Exception {
    try {
      this.provider.defaultFor(TestRoot.type, "element/foo");
      fail("Didn't get XMLE on incorrect path");
    } catch (XmlException xmle) {
      // ok
    }
  }

  public void testPossibleXPaths() throws Exception {
    assertTrue(this.provider.possibleForXPathToHaveDefault("foo"));
    assertTrue(this.provider.possibleForXPathToHaveDefault("foo/bar"));
    assertTrue(this.provider.possibleForXPathToHaveDefault("foo/bar/baz"));

    assertFalse(this.provider.possibleForXPathToHaveDefault("foo/-bar"));
    assertFalse(this.provider.possibleForXPathToHaveDefault("/foo/bar"));
    assertFalse(this.provider.possibleForXPathToHaveDefault("foo/b*ar"));
    assertFalse(this.provider.possibleForXPathToHaveDefault("foo/*"));
    assertFalse(this.provider.possibleForXPathToHaveDefault("foo/bar/../baz"));
  }

  private void checkNoDefault(String xpath) throws Exception {
    assertTrue(this.provider.possibleForXPathToHaveDefault(xpath));
    assertFalse(this.provider.hasDefault(TestRoot.type, xpath));
    try {
      this.provider.defaultFor(TestRoot.type, xpath);
      fail("Didn't get XMLE on element with no default ('" + xpath + "')");
    } catch (XmlException xmle) {
      // ok
    }
  }

  public void testNoDefault() throws Exception {
    checkNoDefault("element/inner-1");
    checkNoDefault("element/inner-4/complex-1");
    checkNoDefault("element/inner-4/complex-3");
    checkNoDefault("element/@attr3");
    checkNoDefault("element/@attr4");
  }

  public void testComplexType() throws Exception {
    try {
      this.provider.defaultFor(TestRoot.type, "element/inner-4");
      fail("Didn't get XMLE on complex-typed element");
    } catch (XmlException xmle) {
      // ok
    }
  }

}
