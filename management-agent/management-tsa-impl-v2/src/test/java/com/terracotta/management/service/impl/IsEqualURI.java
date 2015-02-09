/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.terracotta.management.service.impl;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Matches two URIs. They are deemed equal if they're identical with the exception of the query string:
 * as long as they both contain the same parameters with the same values in any order, the query strings
 * are also considered equals.
 * <p>
 * For instance, these two URIs are equal:<br/><br/>
 * tc-management-api/v2/agents/statistics/servers;names=s2?show=stat1&show=stat3<br/>
 * tc-management-api/v2/agents/statistics/servers;names=s2?show=stat3&show=stat1
 * </p>
 * <p>
 * and these two as well:<br/><br/>
 * tc-management-api/v2/agents/statistics/servers;names=s2?show=stat1,stat3<br/>
 * tc-management-api/v2/agents/statistics/servers;names=s2?show=stat3,stat1<br/>
 * </p>
 *
 * @author Ludovic Orban
 */
public class IsEqualURI extends BaseMatcher<URI> {
  private final URI expectedValue;

  public IsEqualURI(URI expectedValue) {
    this.expectedValue = expectedValue;
  }

  @Override
  public boolean matches(Object item) {
    if (item instanceof URI) {
      URI otherURI = (URI) item;
      boolean eq = true;

      eq &= equals(expectedValue.getScheme(), otherURI.getScheme());
      eq &= equals(expectedValue.getHost(), otherURI.getHost());
      eq &= equals(expectedValue.getPort(), otherURI.getPort());
      eq &= equals(expectedValue.getPath(), otherURI.getPath());
      eq &= queryStringEquals(expectedValue.getQuery(), otherURI.getQuery());

      return eq;
    }
    return false;
  }

  private static boolean queryStringEquals(String qs1, String qs2) {
    if (qs1 == qs2) {
      return true;
    }
    if (qs1 == null || qs2 == null) {
      return false;
    }

    return queryStringToMultimap(qs1).equals(queryStringToMultimap(qs2));
  }

  private static Map<String, Set<String>> queryStringToMultimap(String qs) {
    Map<String, Set<String>> qs1Params = new HashMap<String, Set<String>>();
    {
      List<String> qs1Args = Arrays.asList(qs.split("\\&"));
      for (String qs1Arg : qs1Args) {
        String[] split = qs1Arg.split("\\=");
        Set<String> strings = qs1Params.get(split[0]);
        if (strings == null) {
          strings = new HashSet<String>();
          qs1Params.put(split[0], strings);
        }
        strings.addAll(Arrays.asList(split[1].split("\\,")));
      }
    }
    return qs1Params;
  }

  private static boolean equals(Object o1, Object o2) {
    if (o1 == o2) {
      return true;
    }
    return o1 != null && o1.equals(o2);
  }

  @Override
  public void describeTo(Description description) {
    description.appendValue(expectedValue);
  }

  @Factory
  public static Matcher<URI> equalToUri(URI operand) {
    return new IsEqualURI(operand);
  }

}
