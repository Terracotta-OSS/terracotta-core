/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class AbbreviatedMapListPrettyPrintTest {
  
  public AbbreviatedMapListPrettyPrintTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }
  
  @Test
  public void testPrint() {
    Map<String, Object> map = new LinkedHashMap<>();
    Map<String, Object> sub = new LinkedHashMap<>();
    map.put("map", sub);
    sub.put("test", "value");
    sub.put("number", 1);
    List<Integer> sublist = new ArrayList<>();
    sublist.add(1);
    sublist.add(2);
    sublist.add(3);
    sublist.add(4);
    sub.put("nl", sublist);
    List<String> values = new ArrayList<String>();
    values.add("a1");
    values.add("a2");
    values.add("a3");
    map.put("array",values);
    System.out.println(new AbbreviatedMapListPrettyPrint().println(map).toString());
  }

}
