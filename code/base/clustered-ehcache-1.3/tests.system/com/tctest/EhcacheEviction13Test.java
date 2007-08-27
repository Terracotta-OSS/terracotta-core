package com.tctest;

public class EhcacheEviction13Test extends TransparentTestBase {
	public EhcacheEviction13Test() {
		// disableAllUntil("2007-08-01");
	}

	public void doSetUp(final TransparentTestIface tt) throws Exception {
		tt.getTransparentAppConfig().setClientCount(
				EhcacheEvictionTestApp.EXPECTED_THREAD_COUNT);
		tt.initializeTestRunner();
	}

	protected Class getApplicationClass() {
		return EhcacheEvictionTestApp.class;
	}
}
