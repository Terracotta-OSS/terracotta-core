package com.tctest;

public final class RAMDirectoryTest extends TransparentTestBase {
	public void doSetUp(final TransparentTestIface tt) throws Exception {
		tt.getTransparentAppConfig().setClientCount(
				RAMDirectoryTestApp.EXPECTED_THREAD_COUNT);
		tt.initializeTestRunner();
	}

	protected Class getApplicationClass() {
		return RAMDirectoryTestApp.class;
	}
}
