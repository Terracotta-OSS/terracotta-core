package com.tctest.rife.tests;

import java.util.HashMap;

import org.xml.sax.SAXException;

import junit.framework.Test;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.uwyn.rife.engine.ReservedParameters;
import com.uwyn.rife.servlet.RifeFilter;

public class ContinuationsTest extends AbstractTwoServerDeploymentTest {
	public static Test suite() {
		return new ContinuationsTestSetup();
	}

	public void testPause() throws Exception {
    WebConversation conversation = new WebConversation();
    
    // start counting on the first server, creating the first continuation
    // in this conversation, counter is 0
    WebResponse response1a = server1.ping("/continuations/counter", conversation);
    String count1a = getCurrentCount(response1a);
    assertEquals("0", count1a);
    WebForm form1a = getCountForm(response1a);
    String contid1a = getFormContinuationId(form1a);
    
    // increase the count on the first server, counter is now 1
    WebResponse response1b = form1a.submit();
    String count1b = getCurrentCount(response1b);
    assertEquals("1", count1b);
    WebForm form1b = getCountForm(response1b);
    String contid1b = getFormContinuationId(form1b);
    
    // start counting on the second server, creating the first continuation
    // in a new converstaion on the second server, the counter there starts
    // at 0
    WebResponse response2a = server2.ping("/continuations/counter", conversation);
    String count2a = getCurrentCount(response2a);
    assertEquals("0", count2a);
    WebForm form2a = getCountForm(response2a);
    String contid2a = getFormContinuationId(form2a);
    
    // modify the request to the second server so that it will use the continuation
    // ID from the last response that was sent by the first server, this should
    // resume the count that was active on the first server
    WebRequest request2a = form2a.newUnvalidatedRequest();
    request2a.setParameter(ReservedParameters.CONTID, contid1b);
    
    // resume the conversation of the first server, the count should be 2
    WebResponse response2b = conversation.getResponse(request2a);
    String count2b = getCurrentCount(response2b);
    assertEquals("2", count2b);
    WebForm form2b = getCountForm(response2b);
    String contid2b = getFormContinuationId(form2b);
    
    // ensure that this created a totally new continuation
    assertNotEquals(contid1a, contid2b);
    assertNotEquals(contid1b, contid2b);
    assertNotEquals(contid2a, contid2b);
    
    // increase the count on the second server, counter is now 3
    WebResponse response2c = form2b.submit();
    assertEquals("3", getCurrentCount(response2c));

    // increase the count on the second server, counter is now 4
    WebResponse response2d = getCountForm(response2c).submit();
    assertEquals("4", getCurrentCount(response2d));

    // increase the count on the second server, counter is now 5
    WebResponse response2e = getCountForm(response2d).submit();
    WebForm form2e = getCountForm(response2e);
    assertEquals("5", getCurrentCount(response2e));
    
    // increase the count on the first server, where is was left last time
    // this should be isolated and the count in that branch of the conversion
    // will be 2 after increasing
    WebForm form1c = getCountForm(response1b);
    WebResponse response1c = form1c.submit();
    assertEquals("2", getCurrentCount(response1c));

    // modify the last request to the first server to use the continuation
    // ID that was returned in the last response of the second server, this
    // should resume the count that was active on the second server, counter
    // is now 6
    WebRequest request1d = form1c.newUnvalidatedRequest();
    request1d.setParameter(ReservedParameters.CONTID, getFormContinuationId(form2e));
    WebResponse response1e = conversation.getResponse(request1d);
    assertEquals("6", getCurrentCount(response1e));

    // increase the count on the first server, counter is now 7
    WebResponse response1f = getCountForm(response1e).submit();
    assertEquals("7", getCurrentCount(response1f));

    // increase the count on the first server, counter is now 8
    WebResponse response1g = getCountForm(response1f).submit();
    assertEquals("8", getCurrentCount(response1g));

    // increase the count on the first server, counter is now 9
    WebResponse response1h = getCountForm(response1g).submit();
    assertEquals("9", getCurrentCount(response1h));

    // increase the count on the first server, counting will be finished now
    WebResponse response1i = getCountForm(response1h).submit();
    WebForm form1i = getCountForm(response1i);
    assertNull(form1i);
    assertEquals("finished", getCurrentCount(response1i));
	}

	private String getCurrentCount(WebResponse response) throws SAXException {
		return response.getElementWithID("currentcount").getText();
	}

	private WebForm getCountForm(WebResponse response) throws SAXException {
		return response.getFormWithID("count");
	}

	private String getFormContinuationId(WebForm form) {
		return form.getParameterValue(ReservedParameters.CONTID);
	}

	private static class ContinuationsTestSetup extends TwoServerTestSetup {
		private ContinuationsTestSetup() {
			super(ContinuationsTest.class, "/tc-config-files/continuations-tc-config.xml", "continuations");
		}

		@SuppressWarnings({ "serial", "unchecked" })
		protected void configureWar(DeploymentBuilder builder) {
      builder
      	.addDirectoryOrJARContainingClass(com.uwyn.rife.Version.class)  // rife-1.6.0.jar
      	.addFilter("RIFE", "/*", RifeFilter.class, new HashMap() {{ put("rep.path", "rife-config-files/continuations/participants.xml"); }})
      	.addResourceFullpath("/web-resources", "counter.html", "/WEB-INF/classes/counter.html");
		}
	}
}