package com.tctest.rife.tests;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;

import junit.framework.Test;

import org.xml.sax.SAXException;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.util.runtime.Vm;
import com.tctest.rife.elements.AllTypes;
import com.uwyn.rife.engine.ReservedParameters;
import com.uwyn.rife.servlet.RifeFilter;
import com.uwyn.rife.tools.StringUtils;

public class ContinuationsTest extends AbstractTwoServerDeploymentTest {
	public static Test suite() {
		return new ContinuationsTestSetup();
	}

  public ContinuationsTest() {
    if (Vm.isIBM()) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }
  
  public boolean shouldDisable() {
  	return super.shouldDisable() || Vm.isIBM();
  }
  
	/**
	 * This test simply counts to 10 inside a while loop.
	 * The execution is paused in the middle of the loop so that the user can
	 * press a button on the form to increase the count.
	 * This test will migrate continuations between servers and check if earlier
	 * continuations are properly isolated from newer ones.
	 */
	public void testPause() throws Exception {
		WebConversation conversation = new WebConversation();

		// start counting on the first server, creating the first continuation
		// in this conversation, counter is 0
		WebResponse response1a = server1.ping("/continuations-test/counter", conversation);
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
		// in a new conversation on the second server, the counter there starts
		// at 0
		WebResponse response2a = server2.ping("/continuations-test/counter", conversation);
		String count2a = getCurrentCount(response2a);
		assertEquals("0", count2a);
		WebForm form2a = getCountForm(response2a);
		String contid2a = getFormContinuationId(form2a);

		// modify the request to the second server so that it will use the
		// continuation
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

	/**
	 * This test uses pause() and stepback() on the server to simulate
	 * a loop, it will continue until the total counter reaches 50
	 * the counter starts at 0 and will increase by the value that is
	 * sent through the form.
	 */
	public void testStepBack() throws Exception {
		final String url = "/continuations-test/stepback";
		
		WebForm form;

		// start the addition by accessing the first server
		WebResponse response1 = doQueryStringRequest(server1, url, null);
		form = response1.getFormWithName("getanswer");
		assertEquals(": true", response1.getTitle());
		assertNotNull(form);
		form.setCheckbox("start", true);

		// submit the first value to the second server
		WebResponse response2 = doQueryStringRequest(server2, url, form);
		assertEquals("0 : false", response2.getTitle());
		form = response2.getFormWithName("getanswer");
		assertNotNull(form);
		form.setParameter("answer", "12");

		// submit the second value to the first server
		WebResponse response3 = doQueryStringRequest(server1, url, form);
		assertEquals("12 : true", response3.getTitle());
		form = response3.getFormWithName("getanswer");
		assertNotNull(form);
		form.setParameter("answer", "32");
		
		// submit the third value to the second server
		WebResponse response4 = doQueryStringRequest(server2, url, form);
		assertEquals("44 : true", response4.getTitle());
		form = response4.getFormWithName("getanswer");
		assertNotNull(form);
		form.setParameter("answer", "41");
		
		// submit the fourth value to the first server, this will exceed the
		// total amount of 50 and thus print out the final message
		WebResponse response5 = doQueryStringRequest(server1, url, form);
		assertEquals("got a total of 85 : false", response5.getTitle());
	}
	
	private WebResponse doQueryStringRequest(WebApplicationServer server, String location, WebForm form) throws MalformedURLException, IOException, SAXException {
		return server.ping(location+(null == form ? "" : "?"+form.newUnvalidatedRequest().getQueryString()));
	}

	/**
	 * This test contains operations on all basic JDK types as member fields,
	 * static fields and local variables in an element that uses continuations.
	 * It switches between servers to ensure that all the types are correctly
	 * propagated through the shared roots. 
	 */
	public void testAllTypes() throws Exception {
		WebResponse response = null;
		String text = null;
		String[] lines = null;

		response = server1.ping("/continuations-test/alltypes");

		WebApplicationServer[] servers = new WebApplicationServer[] {server1, server2}; 
		for (int i = 8; i < 40; i++) {
			text = response.getText();
			lines = StringUtils.splitToArray(text, "\n");
			assertEquals(2, lines.length);
			assertEquals(AllTypes.BEFORE + " while " + i, lines[0]);

			response = servers[i%2].ping("/continuations-test/alltypes?" + ReservedParameters.CONTID + "=" + lines[1]);
		}

		text = response.getText();
		lines = StringUtils.splitToArray(text, "\n");
		assertEquals(2, lines.length);
		assertEquals(AllTypes.BEFORE + " a", lines[0]);

		response = server1.ping("/continuations-test/alltypes?" + ReservedParameters.CONTID + "=" + lines[1]);

		text = response.getText();
		lines = StringUtils.splitToArray(text, "\n");
		assertEquals(2, lines.length);
		assertEquals(AllTypes.BEFORE + " b", lines[0]);

		response = server2.ping("/continuations-test/alltypes?" + ReservedParameters.CONTID + "=" + lines[1]);

		text = response.getText();
		lines = StringUtils.splitToArray(text, "\n");
		assertEquals(2, lines.length);
		assertEquals(AllTypes.BEFORE + " c", lines[0]);

		response = server1.ping("/continuations-test/alltypes?" + ReservedParameters.CONTID + "=" + lines[1]);

		assertEquals(
				"40,1209000,11,16,7,8,\n"
						+ "9223372036854775807,0,9223372036854775709,922337203685477570,8,-1,99,\n"
						+ "0.4,8.4,-80.4,-80.0,0.0,-1.0,\n"
						+ "2389.98,2407.3799996185303,-10.0,-1.0,-0.0,2397.3799996185303,\n"
						+ "local ok,some value 6899,\n"
						+ "true|false|false,K|O,54.7|9.8,82324.45|997823.23|87.8998,98|12,8|11,\n"
						+ "111111|444444|666666|999999,111111|444444|666666|999999,333|8888|99,333|66|99,\n"
						+ "zero|one|two|null,zero|one|two|null,ini|mini|moo,\n"
						+ "3:str 0 0|replaced|str 0 2|str 0 3||str 1 0|str 1 1|str 1 2|str 1 3||str 2 0|str 2 1|str 2 2|str 2 3,\n"
						+ "3:str 0 0|replaced|str 0 2|str 0 3||str 1 0|str 1 1|str 1 2|str 1 3||str 2 0|str 2 1|str 2 2|str 2 3,\n"
						+ "2:str 0 0|str 0 1||str 1 0|str 1 1,\n"
						+ "-98|97,-98|97,98|23|11,\n"
						+ "2:0|1|2|3|4||100|101|102|-89|104,\n"
						+ "2:0|1|2|3|4||100|101|102|-89|104,\n"
						+ "3:0|1|2||100|101|102||200|201|202,\n"
						+ "2,4,member ok,8111|8333,2:31|32|33|34||35|36|37|38,\n"
						+ "1,3,static ok,9111|9333,3:1|2|3|4||5|6|7|8||9|10|11|12,\n"
						+ "2,4,member ok,8111|8333,2:31|32|33|34||35|36|37|38,\n"
						+ "1,3,static ok,9111|9333,3:1|2|3|4||5|6|7|8||9|10|11|12,\n"
						+ "100,400,member ok two,8333|8111|23687,1:35|36|37|38,\n"
						+ "60,600,static ok two,23476|9333|9111|8334,2:9|10|11|12||1|2|3|4,\n"
						+ "2:3:3:0|1|2|3|4|5|6|7||10|11|12|13|14|15|16|17||20|21|22|23|24|25|26|27|||100|101|102|103|104|105|106|107||110|111|112|113|114|115|116|117||120|121|122|123|-99|null|126|127,\n"
						+ "2:3:3:0|1|2|3|4|5|6|7||10|11|12|13|14|15|16|17||20|21|22|23|24|25|26|27|||100|101|102|103|104|105|106|107||110|111|112|113|114|115|116|117||120|121|122|123|-99|null|126|127,\n"
						+ "4:1|3||5|7||11|-199||17|19,\n" + "4:1|3||5|7||11|-199||17|19,\n" + "me value 6899,\n"
						+ "2147483647,25,4,109912,118,-2147483648", response.getText());
	}

	/**
	 * This test checks if continuations also work over a cluster when the
	 * element doesn't extend Element, but rather implements the
	 * ElementAware interface.
	 */
	public void testSimpleInterface() throws Exception {
		WebResponse response;

		response = server1.ping("/continuations-test/simpleinterface");

		String text = response.getText();
		String[] lines = StringUtils.splitToArray(text, "\n");
		assertEquals(2, lines.length);
		assertEquals("before simple pause", lines[0]);

		response = server2.ping("/continuations-test/simpleinterface?" + ReservedParameters.CONTID + "=" + lines[1]);

		assertEquals("after simple pause", response.getText());
	}

	/**
	 * This test checks if call/answer continuations work correctly over
	 * several nodes.
	 */
	public void testCallAnswer() throws Exception {
		WebResponse response;

		response = server1.ping("/continuations-test/callanswer");

		String text = response.getText();
		String[] lines = StringUtils.splitToArray(text, "\n");
		assertEquals(6, lines.length);
		assertEquals("before call", lines[0]);
		assertEquals("the data:somevalue", lines[2]);
		assertEquals("before answer", lines[3]);
		assertEquals("the exit's answer", lines[4]);
		assertEquals("after call", lines[5]);

		response = server2.ping("/continuations-test/callanswer?" + ReservedParameters.CONTID + "=" + lines[1]);

		assertEquals("after call", response.getText());
	}

	private static class ContinuationsTestSetup extends TwoServerTestSetup {
		private ContinuationsTestSetup() {
			super(ContinuationsTest.class, "/tc-config-files/continuations-tc-config.xml", "continuations-test");
		}

		@SuppressWarnings( { "serial", "unchecked" })
		protected void configureWar(DeploymentBuilder builder) {
			builder
				.addDirectoryOrJARContainingClass(com.uwyn.rife.Version.class) // rife jar
				.addFilter("RIFE", "/*", RifeFilter.class, new HashMap() {{ put("rep.path", "rife-config-files/continuations/participants.xml"); }})
				.addResourceFullpath("/web-resources", "counter.html", "WEB-INF/classes/counter.html")
				.addResourceFullpath("/web-resources", "stepback.html", "WEB-INF/classes/stepback.html");
		}
	}
}
