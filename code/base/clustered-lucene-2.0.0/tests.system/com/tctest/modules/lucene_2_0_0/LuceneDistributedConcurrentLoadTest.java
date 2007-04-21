package com.tctest.modules.lucene_2_0_0;

import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class LuceneDistributedConcurrentLoadTest extends TransparentTestBase {

	public LuceneDistributedConcurrentLoadTest() {
		disableAllUntil("2008-02-28");
	}

	private static final int TIMEOUT = 10 * 60 * 1000; // 10min;

	protected Class getApplicationClass() {
		// return LuceneDistributedConcurrentLoadTestApp.class;
		return null;
	}

	public void doSetUp(TransparentTestIface t) throws Exception {
		t.getTransparentAppConfig().setClientCount(2);
		t.getTransparentAppConfig().setIntensity(1);
		t.getRunnerConfig().setExecutionTimeout(TIMEOUT);
		t.initializeTestRunner();
	}
}
