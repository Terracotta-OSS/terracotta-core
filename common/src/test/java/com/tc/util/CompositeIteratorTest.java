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
package com.tc.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.concurrent.ThreadLocalRandom.current;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CompositeIteratorTest {
  @Test
  public void testIteration() {
    List<Integer> first = current().ints(5).boxed().collect(toList());
    List<Integer> second = current().ints(10).boxed().collect(toList());

    CompositeIterator<Integer> iterator = new CompositeIterator<>(asList(first.iterator(), second.iterator()));
    List<Integer> actual = new ArrayList<>();
    iterator.forEachRemaining(actual::add);

    List<Integer> expected = new ArrayList<>();
    expected.addAll(first);
    expected.addAll(second);

    assertThat(actual, is(equalTo(expected)));
  }
}