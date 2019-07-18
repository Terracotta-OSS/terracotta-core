/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
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
