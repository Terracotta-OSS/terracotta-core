package org.terracotta.modules.lucene_2_0_0;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.demo.IndexHTML;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.store.RAMDirectory;

import com.tc.util.ZipBuilder;

public final class LuceneSampleDataIndex {

  private static final String HTML_DATA = "sample-html-data-1.0.zip";
  private static final String DATA_DIR  = "bible";

  private final String        indexPath;
  private final RAMDirectory  directory;

  public LuceneSampleDataIndex(File workingDir) throws IOException {
    this.indexPath = workingDir + File.separator + DATA_DIR;
    if (!new File(indexPath).exists()) {
      String dataPath = LuceneSampleDataIndex.class.getResource(HTML_DATA).getPath();
      BufferedInputStream in = new BufferedInputStream(new FileInputStream(dataPath));
      ZipBuilder.unzip(in, workingDir);
      String[] args = new String[] { "-create", "-index", indexPath, workingDir.getAbsolutePath() };
      IndexHTML.main(args);
    }
    directory = new RAMDirectory(indexPath);
  }

  public Hits query(String queryString) throws IOException, ParseException {
    Searcher searcher = new IndexSearcher(directory);
    Analyzer analyzer = new StandardAnalyzer();
    QueryParser parser = new QueryParser("contents", analyzer);
    Query query = parser.parse(queryString);
    return searcher.search(query);
  }

  public void put(String field, String value) throws Exception {
    Document doc = new Document();
    doc.add(new Field(field, value, Field.Store.YES, Field.Index.TOKENIZED));
    final IndexWriter writer = new IndexWriter(directory, new StandardAnalyzer(), (directory.list().length == 0));
    writer.addDocument(doc);
    writer.optimize();
    writer.close();
  }
}
