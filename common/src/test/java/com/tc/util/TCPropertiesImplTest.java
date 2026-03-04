/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.util;

import com.tc.properties.TCProperties;
import org.apache.commons.io.IOUtils;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import junit.framework.TestCase;
import org.hamcrest.Matchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class TCPropertiesImplTest extends TestCase {

  public void testLoad() {
    TCProperties propertyStore = TCPropertiesImpl.getProperties();
    assertThat(propertyStore.getProperty(TCPropertiesConsts.L2_ELECTION_TIMEOUT), notNullValue());
  }

  public void testTrim() {
    TCProperties properties = TCPropertiesImpl.getProperties();
    properties.setProperty("tim1", "eck");
    properties.setProperty("tim2", " eck");
    properties.setProperty("tim3", "eck ");
    properties.setProperty("tim4", " eck ");

    assertThat(properties.getProperty("tim1"), is("eck"));
    assertThat(properties.getProperty("tim2"), is("eck"));
    assertThat(properties.getProperty("tim3"), is("eck"));
    assertThat(properties.getProperty("tim4"), is("eck"));
  }

  public void testUnset() {
    TCProperties properties = TCPropertiesImpl.getProperties();
    properties.setProperty("a", "b");
    properties.setProperty("a", null);
    assertNull(properties.getProperty("a", true));
  }

  public void testKeyCaseBehavior() {
    TCProperties properties = TCPropertiesImpl.getProperties();

    properties.setProperty("Tim", "eck1");
    assertEquals("eck1", properties.getProperty("tim"));
    Properties export = new Properties();
    properties.addAllPropertiesTo(export);
    assertThat(export.keySet(), hasItem("tim"));
    assertThat(export.keySet(), not(hasItem("Tim")));

    properties.setProperty("TIM", "eck2");
    assertEquals("eck2", properties.getProperty("tim"));
    properties.addAllPropertiesTo(export);
    assertThat(export.keySet(), hasItem("tim"));
    assertThat(export.keySet(), not(hasItem("TIM")));

    properties.setProperty("TiM", "eck3");
    assertEquals("eck3", properties.getProperty("tim"));
    properties.addAllPropertiesTo(export);
    assertThat(export.keySet(), hasItem("tim"));
    assertThat(export.keySet(), not(hasItem("TiM")));
  }

  public void testSetAndGet() {
    TCProperties properties = TCPropertiesImpl.getProperties();

    properties.setProperty("xyz", "abc");
    Assert.assertEquals("abc", properties.getProperty("XyZ"));
    Assert.assertEquals("abc", properties.getProperty("xYZ"));
    properties.setProperty("abc", "def");
    Assert.assertEquals("def", properties.getProperty("AbC"));
  }
}
