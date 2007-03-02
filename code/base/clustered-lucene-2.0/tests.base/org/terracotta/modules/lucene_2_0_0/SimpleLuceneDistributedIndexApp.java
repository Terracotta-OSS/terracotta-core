package org.terracotta.modules.lucene_2_0_0;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.test.TempDirectoryHelper;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

public final class SimpleLuceneDistributedIndexApp extends AbstractTransparentApp {

  private static final String SEARCH_FIELD = "contents";
  private final CyclicBarrier barrier;

  public SimpleLuceneDistributedIndexApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      boolean writerNode = false;
      if (barrier.await() == 0) writerNode = true;
      LuceneSampleDataIndex index = null;
      if (writerNode) index = new LuceneSampleDataIndex(getTempDirectory(true));
      barrier.await();
      if (!writerNode) index = new LuceneSampleDataIndex(getTempDirectory(false));
      barrier.await();

      int count = index.query("buddha").length();
      Assert.assertEquals(count, 0);
      barrier.await();
      if (writerNode) index.put(SEARCH_FIELD, "buddha");
      barrier.await();
      count = index.query("buddha").length();
      Assert.assertEquals(count, 1);
      count = index.query("lamb").length();
      Assert.assertEquals(count, 14);
      barrier.await();
      if (writerNode) index.put(SEARCH_FIELD, "Mary had a little lamb.");
      barrier.await();
      count = index.query("lamb").length();
      Assert.assertEquals(count, 15);

    } catch (BrokenBarrierException e) {
      barrier.reset();
      notifyError(e);
    } catch (InterruptedException e) {
      barrier.reset();
      notifyError(e);
    } catch (Exception e) {
      barrier.reset();
      notifyError(e);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    config.addIncludePattern(CyclicBarrier.class.getName());
    config.addIncludePattern("org.apache.lucene.store..*");
    config.addIncludePattern(LuceneSampleDataIndex.class.getName());
    String className = SimpleLuceneDistributedIndexApp.class.getName();
    config.addIncludePattern(className);
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");
    config.addWriteAutolock("* " + LuceneSampleDataIndex.class.getName() + "*.*(..)");
    config.addRoot("directory", LuceneSampleDataIndex.class.getName() + ".directory");
    config.addRoot("barrier", className + ".barrier");
  }

  private File getTempDirectory(boolean clean) throws IOException {
    return new TempDirectoryHelper(getClass(), clean).getDirectory();
  }
}
