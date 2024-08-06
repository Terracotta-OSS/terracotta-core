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