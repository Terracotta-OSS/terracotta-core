/*
 @COPYRIGHT@
 */
package demo.sharedqueue;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;

public class SimpleHttpHandler extends AbstractHandler {

	private Queue queue;

	public static final String ACTION = "/webapp";

	public static final String UNITS_OF_WORK = "unitsOfWork";

	public SimpleHttpHandler(Queue queue) {
		this.queue = queue;
	}

	private final int getIntForParameter(HttpServletRequest request, final String name) {
		String param = request.getParameter(name);
		try {
			return param == null ? 0 : Integer.parseInt(param);
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}
	
	public final void handle(String pathInContext, HttpServletRequest request,
			HttpServletResponse response, int dispatch) throws IOException,
			ServletException {
		Request base_request = (request instanceof Request) ? 
				(Request)request :
				HttpConnection.getCurrentConnection().getRequest();
			
		if (pathInContext.equals("/add_work_item.asp")) {
			final int unitsOfWork = getIntForParameter(request, UNITS_OF_WORK);
			Thread processor = new Thread(new Runnable() {
				public void run() {
					for (int i = 0; i < unitsOfWork; i++) {
						queue.addJob();
						try {
							Thread.sleep(50);
						}
						// added slight delay to improve visuals
						catch (InterruptedException ie) {
							System.err.println(ie.getMessage());
						}
					}
				}
			});
			processor.start();
			response.sendRedirect(ACTION);
		}
		else if (pathInContext.equals("/get_info_xml.asp")) {
			response.setContentType("text/xml");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
			response.getWriter().println("<root>");
			response.getWriter().println(queue.getXmlData());
			response.getWriter().println("</root>");
			base_request.setHandled(true);
		}
	}
}
