/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import junit.framework.TestCase;

/**
 * This class testing the following "designed" behavior
 * 1. App name, config path and distributed events supports wildcards (heading and tailing only though)
 * 2. Bean name, excluded field name has to match exactly
 * 
 */
public class StandardDSOSpringConfigHelperTest extends TestCase {
  private StandardDSOSpringConfigHelper config = null;
  
  public void setUp() {
    config = new StandardDSOSpringConfigHelper();
  }
  
  /*
   * Test method for 'com.tc.object.config.StandardDSOSpringConfigHelper.isMatching(String, String)'
   */
  public void testIsMatching() {
    assertTrue(config.isMatching("*", null));
    assertTrue(config.isMatching("*", ""));
    assertTrue(config.isMatching("*", "a"));
    assertTrue(config.isMatching("*", "aaaa"));

    assertTrue(config.isMatching("*a", "a"));
    assertTrue(config.isMatching("*a", "ba"));
    assertTrue(config.isMatching("*aaa", "baaa"));
    assertTrue(config.isMatching("*aaa", "baaaa")); // ends with
    assertFalse(config.isMatching("*a", "b"));
    assertFalse(config.isMatching("*a", null));
    assertFalse(config.isMatching("*aaa", "baa"));

    assertTrue(config.isMatching("a*", "a"));
    assertTrue(config.isMatching("a*", "ab"));
    assertTrue(config.isMatching("a*", "aab"));
    assertTrue(config.isMatching("aa*", "aab"));
    assertTrue(config.isMatching("aa*", "aaab"));
    assertFalse(config.isMatching("a*", null));
    assertFalse(config.isMatching("a*", "b"));
    assertFalse(config.isMatching("aaa*", "aab"));

    assertTrue(config.isMatching("**", ""));
    assertTrue(config.isMatching("*a*", "a"));
    assertTrue(config.isMatching("*a*", "aaa"));
    assertTrue(config.isMatching("*a*", "bab"));
    assertTrue(config.isMatching("*a*", "ba"));
    assertTrue(config.isMatching("*a*", "ab"));
    assertTrue(config.isMatching("*a*", "baaab"));
    assertTrue(config.isMatching("*a*", "aaab"));
    assertTrue(config.isMatching("*a*", "baaa"));
    assertFalse(config.isMatching("*a*", ""));
    assertFalse(config.isMatching("**", null));

    assertTrue(config.isMatching("", ""));
    assertTrue(config.isMatching("aa", "aa"));
    assertFalse(config.isMatching("aa", "aaa"));
    assertFalse(config.isMatching("", "aa"));
  }
  
  public void testMatchingConfig() throws Exception {
    config.addConfigPattern("exact");
    config.addConfigPattern("*Tail");
    config.addConfigPattern("Head*");
    config.addConfigPattern("*Wherever*");
    
    assertTrue(config.isMatchingConfig("exact"));
    assertTrue(config.isMatchingConfig("Blah.And_Tail"));
    assertTrue(config.isMatchingConfig("Head.And_Blah"));
    assertTrue(config.isMatchingConfig("Blah.WhereverAnd_Blah"));
    assertTrue(config.isMatchingConfig("Tail"));
    assertTrue(config.isMatchingConfig("Head"));
    assertTrue(config.isMatchingConfig("itisWherever"));
    assertTrue(config.isMatchingConfig("Whereveritgoes"));
    assertTrue(config.isMatchingConfig("Wherever"));
    
    assertFalse(config.isMatchingConfig("notexact"));
    assertFalse(config.isMatchingConfig("withTailandmore"));
    assertFalse(config.isMatchingConfig("notHead"));
    assertFalse(config.isMatchingConfig("WHereverhastobecasesensitive"));
  }
  
  public void testDistributedEvents() throws Exception {
    config.addDistributedEvent("exact");
    config.addDistributedEvent("*Tail");
    config.addDistributedEvent("Head*");
    config.addDistributedEvent("*Wherever*");
    
    assertTrue(config.isDistributedEvent("exact"));
    assertTrue(config.isDistributedEvent("Blah.And_Tail"));
    assertTrue(config.isDistributedEvent("Head.And_Blah"));
    assertTrue(config.isDistributedEvent("Blah.WhereverAnd_Blah"));
    assertTrue(config.isDistributedEvent("Tail"));
    assertTrue(config.isDistributedEvent("Head"));
    assertTrue(config.isDistributedEvent("itisWherever"));
    assertTrue(config.isDistributedEvent("Whereveritgoes"));
    assertTrue(config.isDistributedEvent("Wherever"));
    
    assertFalse(config.isDistributedEvent("notexact"));
    assertFalse(config.isDistributedEvent("withTailandmore"));
    assertFalse(config.isDistributedEvent("notHead"));
    assertFalse(config.isDistributedEvent("WHereverhastobecasesensitive"));
  }
  
  public void testDistributedBean() throws Exception {
    config.addBean("exact");
    config.addBean("*");
    
    assertTrue(config.isDistributedBean("exact"));
    assertFalse(config.isDistributedBean("wildcardshouldnotwork"));
  }
  
  public void testApplicationPattern() throws Exception {
    config.addApplicationNamePattern("exact");
    config.addApplicationNamePattern("*Tail");
    config.addApplicationNamePattern("Head*");
    config.addApplicationNamePattern("*Wherever*");
    
    assertTrue(config.isMatchingApplication("exact"));
    assertTrue(config.isMatchingApplication("Blah.And_Tail"));
    assertTrue(config.isMatchingApplication("Head.And_Blah"));
    assertTrue(config.isMatchingApplication("Blah.WhereverAnd_Blah"));
    assertTrue(config.isMatchingApplication("Tail"));
    assertTrue(config.isMatchingApplication("Head"));
    assertTrue(config.isMatchingApplication("itisWherever"));
    assertTrue(config.isMatchingApplication("Whereveritgoes"));
    assertTrue(config.isMatchingApplication("Wherever"));
    
    assertFalse(config.isMatchingApplication("notexact"));
    assertFalse(config.isMatchingApplication("withTailandmore"));
    assertFalse(config.isMatchingApplication("notHead"));
    assertFalse(config.isMatchingApplication("WHereverhastobecasesensitive"));   
  }
  
  public void testExludeField() throws Exception {
    config.addBean("existingBean");
    config.excludeField("existingBean", "exact");
    config.excludeField("existingBean", "*");
    config.excludeField("newBean", "exact");
    config.excludeField("newBean", "*");

    assertFalse(config.isDistributedField("newBean", "exact"));
    assertFalse(config.isDistributedField("existingBean", "exact"));
    
    
    assertTrue(config.isDistributedField("newBean", "wildcardshouldnotwork"));
    assertTrue(config.isDistributedField("existigBean", "wildcardshouldnotwork"));
    assertTrue(config.isDistributedField("beanNameNotMatch", "exact"));
    assertTrue(config.isDistributedField("beanNameNotMatch", "wildcardshouldnotwork"));
  }
}  
