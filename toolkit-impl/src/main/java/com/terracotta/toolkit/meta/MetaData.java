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
package com.terracotta.toolkit.meta;

import com.terracottatech.search.SearchMetaData;

import java.util.Date;
import java.util.Map;

public interface MetaData {

  String getCategory();

  void add(SearchMetaData name, Object value);

  void add(SearchMetaData name, boolean value);

  void add(SearchMetaData name, byte value);

  void add(SearchMetaData name, char value);

  void add(SearchMetaData name, double value);

  void add(SearchMetaData name, float value);

  void add(SearchMetaData name, int value);

  void add(SearchMetaData name, long value);

  void add(SearchMetaData name, short value);

  void add(SearchMetaData name, SearchMetaData value);

  void add(SearchMetaData name, byte[] value);

  void add(SearchMetaData name, Enum value);

  void add(SearchMetaData name, Date value);

  void add(SearchMetaData name, java.sql.Date value);

  void add(String name, Object val);

  void set(SearchMetaData name, Object newValue);

  Map<String, Object> getMetaDatas();

}