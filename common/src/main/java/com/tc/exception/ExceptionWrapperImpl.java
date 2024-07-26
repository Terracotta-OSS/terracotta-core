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
package com.tc.exception;

/**
 * Wrap exception in a nice block message surrounded by ****s
 */
public class ExceptionWrapperImpl implements ExceptionWrapper {

  private static final int MAX_STAR_COUNT = 79;

  @Override
  public String wrap(String message) {
    message = String.valueOf(message);
    int starCount = longestLineCharCount(message);
    if(starCount > MAX_STAR_COUNT) {
      starCount = MAX_STAR_COUNT;
    }
    return "\n" + getStars(starCount) + "\n" + message
           + "\n" + getStars(starCount) + "\n";
  }

  private String getStars(int starCount) {
    StringBuffer stars = new StringBuffer();
    while(starCount-- > 0) {
      stars.append('*');
    }
    return stars.toString();
  }

  private int longestLineCharCount(String message) {
    int count = 0;
    int sidx = 0, eidx = 0;
    while ((eidx = message.indexOf('\n', sidx)) >= 0) {
      if (count < (eidx - sidx)) {
        count = (eidx - sidx);
      }
      sidx = eidx + 1;
    }
    if (sidx < message.length() && count < (message.length() - sidx)) {
      count = (message.length() - sidx);
    }
    return count;
  }

}
