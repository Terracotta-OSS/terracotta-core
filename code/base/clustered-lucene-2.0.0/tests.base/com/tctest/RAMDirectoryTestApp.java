package com.tctest;

import java.util.Iterator;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;

import com.tc.util.Assert;
import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

/**
 * RAMDirectory clustering test
 * @author jgalang
 */
public class RAMDirectoryTestApp extends AbstractErrorCatchingTransparentApp {

	static final int EXPECTED_THREAD_COUNT = 2;

	private final CyclicBarrier barrier;

	private final RAMDirectory clusteredDirectory;

	private final StandardAnalyzer analyzer;

	/**
	 * Inject Lucene 2.0.0 configuration, and instrument this test class
	 * @param visitor
	 * @param config
	 */
	public static void visitL1DSOConfig(final ConfigVisitor visitor,
			final DSOClientConfigHelper config) {
	    config.addNewModule("clustered-lucene", "2.0.0");
		config.addAutolock("* *..*.*(..)", ConfigLockLevel.WRITE);

	    final String testClass = RAMDirectoryTestApp.class.getName();
		final TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
		spec.addRoot("barrier", "barrier");
		spec.addRoot("clusteredDirectory", "clusteredDirectory");
	}

	public RAMDirectoryTestApp(final String appId, final ApplicationConfig cfg,
			final ListenerProvider listenerProvider) {
		super(appId, cfg, listenerProvider);
		clusteredDirectory = new RAMDirectory();
		analyzer = new StandardAnalyzer();
		barrier = new CyclicBarrier(getParticipantCount());
	}
	
	/**
	 * Test that the data written in the clustered RAMDirectory
	 * by one node, becomes available in the other.
	 */
	protected void runTest() throws Throwable {
		if (barrier.await() == 0) {
			addDataToMap("DATA1");
			letOtherNodeProceed();
			waitForPermissionToProceed();
			verifyEntries("DATA2");
		} else {
			waitForPermissionToProceed();
			verifyEntries("DATA1");
			addDataToMap("DATA2");
			letOtherNodeProceed();
		}
		barrier.await();
	}

	// This is lame but it makes runTest() slightly more readable
	private void letOtherNodeProceed() throws InterruptedException,
			BrokenBarrierException {
		barrier.await();
	}

	// This is lame but it makes runTest() slightly more readable
	private void waitForPermissionToProceed() throws InterruptedException,
			BrokenBarrierException {
		barrier.await();
	}

	/**
	 * Add datum to RAMDirectory
	 * @param value The datum to add
	 * @throws Throwable
	 */
	private void addDataToMap(final String value)
			throws Throwable {
		final Document doc = new Document();
		doc.add(new Field("key", value, Field.Store.YES, Field.Index.TOKENIZED));
		doc.add(new Field("value", value, Field.Store.YES,
				Field.Index.TOKENIZED));

		synchronized (clusteredDirectory) {
			final IndexWriter writer = new IndexWriter(this.clusteredDirectory,
					this.analyzer, this.clusteredDirectory.list().length == 0);
			writer.addDocument(doc);
			writer.optimize();
			writer.close();
		}
	}

	/**
	 * Attempt to retrieve datum from RAMDirectory. 
	 * @param value The datum to retrieve
	 * @throws Exception
	 */
	private void verifyEntries(final String value) throws Exception {
		final StringBuffer data = new StringBuffer();
		synchronized (clusteredDirectory) {
			final QueryParser parser = new QueryParser("key", this.analyzer);
			final Query query = parser.parse(value);
			BooleanQuery.setMaxClauseCount(100000);
			final IndexSearcher is = new IndexSearcher(this.clusteredDirectory);
			final Hits hits = is.search(query);

			for (Iterator i = hits.iterator(); i.hasNext();) {
				final Hit hit = (Hit) i.next();
				final Document document = hit.getDocument();
				data.append(document.get("value"));
			}
		}
		Assert.assertEquals(value, data.toString());
	}
}
