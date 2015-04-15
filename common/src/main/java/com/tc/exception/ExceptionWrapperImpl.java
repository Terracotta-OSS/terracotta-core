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
