/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

public interface IndexFile {

  public boolean isTCFile();

  public String getDestFilename();

  public String getLuceneFilename();

  public long length();

  String getIndexId(); // striped idx id, as in <lucene_dir>/<cache_name>/<index_id>/[lucene index files...]
}
