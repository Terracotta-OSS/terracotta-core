/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.util.concurrent;

import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.Set;

import static com.tc.util.Assert.assertFalse;
import static com.tc.util.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.junit.matchers.JUnitMatchers.hasItems;

/**
 * @author tim
 */
public class TCConcurrentMultiMapTest {
  @Test
  public void testAdd() throws Exception {
    TCConcurrentMultiMap<String, String> mmap = new TCConcurrentMultiMap<String, String>();
    assertTrue(mmap.add("foo", "bar"));
    assertTrue(mmap.get("foo").contains("bar"));
    assertFalse(mmap.add("foo", "baz"));
    assertTrue(mmap.get("foo").contains("baz"));
    assertFalse(mmap.remove("foo", "bunk"));
    assertTrue(mmap.get("foo").contains("baz"));

    // Add the same thing twice to make sure we don't crash adding into a singletonSet.
    assertTrue(mmap.add("1", "one"));
    assertFalse(mmap.add("1", "one"));
    assertTrue(mmap.get("1").contains("one"));
  }

  @Test
  public void testRemove() throws Exception {
    TCConcurrentMultiMap<String, String> mmap = new TCConcurrentMultiMap<String, String>();
    mmap.add("foo", "bar");
    mmap.add("foo", "baz");
    assertTrue(mmap.remove("foo", "bar"));
    mmap.add("foo", "buz");
    assertThat(mmap.get("foo"), hasItems("baz", "buz"));
    assertFalse(mmap.get("foo").contains("bar"));
    assertTrue(mmap.remove("foo", "buz"));
    assertFalse(mmap.get("foo").contains("buz"));
    assertTrue(mmap.get("foo").contains("baz"));
    assertFalse(mmap.remove("foo", "bogus"));
    assertTrue(mmap.remove("foo", "baz"));
    assertFalse(mmap.remove("foo", "baz"));
    assertTrue(mmap.get("foo").isEmpty());
  }

  @Test
  public void testRemoveAll() throws Exception {
    TCConcurrentMultiMap<String, String> mmap = new TCConcurrentMultiMap<String, String>();
    mmap.add("foo", "bar");
    mmap.add("foo", "baz");
    mmap.add("foo", "boo");
    assertThat(mmap.removeAll("foo"), hasItems("bar", "baz", "boo"));
    assertTrue(mmap.get("foo").isEmpty());
  }

  @Test
  public void testAddAll() throws Exception {
    TCConcurrentMultiMap<String, String> mmap = new TCConcurrentMultiMap<String, String>();
    assertTrue(mmap.addAll("foo", Sets.newSet("bar", "baz", "boo")));
    assertThat(mmap.get("foo"), hasItems("bar", "baz", "boo"));
    assertTrue(mmap.remove("foo", "boo"));
    assertThat(mmap.get("foo"), hasItems("bar", "baz"));
    assertTrue(mmap.remove("foo", "bar"));
    assertThat(mmap.get("foo"), hasItem("baz"));
    assertFalse(mmap.remove("foo", "bunk"));
    assertThat(mmap.get("foo"), hasItem("baz"));
    assertTrue(mmap.remove("foo", "baz"));
    assertTrue(mmap.get("foo").isEmpty());

    assertTrue(mmap.add("1", "one"));
    assertFalse(mmap.addAll("1", Sets.newSet("two", "three")));
    assertThat(mmap.get("1"), hasItems("one", "two", "three"));
    assertFalse(mmap.addAll("1", Sets.newSet("four", "five")));
    assertThat(mmap.get("1"), hasItems("one", "two", "three", "four", "five"));
  }
}
