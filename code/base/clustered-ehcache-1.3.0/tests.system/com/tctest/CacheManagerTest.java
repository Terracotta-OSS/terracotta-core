package com.tctest;

public class CacheManagerTest extends TransparentTestBase {

	public void doSetUp(final TransparentTestIface tt) throws Exception {
		tt.getTransparentAppConfig().setClientCount(
				CacheManagerTestApp.EXPECTED_THREAD_COUNT);
		tt.initializeTestRunner();
	}

	protected Class getApplicationClass() {
		return CacheManagerTestApp.class;
	}
	
}
