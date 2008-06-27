package org.terracotta.modules.lucene_2_0_0;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.test.TempDirectoryHelper;
import com.tc.util.Assert;
import com.tc.util.TIMUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.io.IOException;

public final class SimpleLuceneDistributedIndexApp extends AbstractTransparentApp {

  private static final String SEARCH_FIELD = "contents";
  private final CyclicBarrier barrier;

  public SimpleLuceneDistributedIndexApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      final boolean writerNode = barrier.barrier() == 0;
      LuceneSampleDataIndex index = null;

      if (writerNode) {
        index = new LuceneSampleDataIndex(getTempDirectory(true));
      }

      barrier.barrier();

      if (!writerNode) {
        index = new LuceneSampleDataIndex(getTempDirectory(false));
      }
      barrier.barrier();

      int count = index.query("buddha").length();
      Assert.assertEquals(count, 0);
      barrier.barrier();
      if (writerNode) index.put(SEARCH_FIELD, "buddha");
      barrier.barrier();
      count = index.query("buddha").length();
      Assert.assertEquals(count, 1);
      count = index.query("lamb").length();
      Assert.assertEquals(count, 14);
      barrier.barrier();
      if (writerNode) index.put(SEARCH_FIELD, "Mary had a little lamb.");
      barrier.barrier();
      count = index.query("lamb").length();
      Assert.assertEquals(count, 15);
    } catch (Exception e) {
      try {
        barrier.barrier();
      } catch (Exception e1) {
        notifyError(e);
      }
      notifyError(e);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    new CyclicBarrierSpec().visit(visitor, config);
    config.addModule(TIMUtil.LUCENE_2_0_0, TIMUtil.getVersion(TIMUtil.LUCENE_2_0_0));

    config.addIncludePattern(LuceneSampleDataIndex.class.getName());
    config.addIncludePattern(SimpleLuceneDistributedIndexApp.class.getName());

    config.addRoot("directory", LuceneSampleDataIndex.class.getName() + ".directory");
    config.addRoot("barrier", SimpleLuceneDistributedIndexApp.class.getName() + ".barrier");
  }

  private File getTempDirectory(boolean clean) throws IOException {
    return new TempDirectoryHelper(getClass(), clean).getDirectory();
  }
}
