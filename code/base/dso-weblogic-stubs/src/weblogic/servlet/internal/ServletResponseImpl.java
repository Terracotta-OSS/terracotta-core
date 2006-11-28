package weblogic.servlet.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

// NOTE: real implementation sometimes has final access, this is removed by instrumentation
public class ServletResponseImpl implements HttpServletResponse {

    protected ServletResponseImpl() {
        // real implementation has package-private
        // constructor, it is made protected at runtime
    }

    // HttpServletResponse methods
    public void addCookie(Cookie arg0) {
        throw new AssertionError();
    }

    public void addDateHeader(String arg0, long arg1) {
        throw new AssertionError();
    }

    public void addHeader(String arg0, String arg1) {
        throw new AssertionError();
    }

    public void addIntHeader(String arg0, int arg1) {
        throw new AssertionError();
    }

    public boolean containsHeader(String arg0) {
        throw new AssertionError();
    }

    public String encodeRedirectURL(String arg0) {
        throw new AssertionError();
    }

    public String encodeRedirectUrl(String arg0) {
        throw new AssertionError();
    }

    public String encodeURL(String arg0) {
        throw new AssertionError();
    }

    public String encodeUrl(String arg0) {
        throw new AssertionError();
    }

    public void sendError(int arg0) throws IOException {
        throw new AssertionError();

    }

    public void sendError(int arg0, String arg1) throws IOException {
        throw new AssertionError();
    }

    public void sendRedirect(String arg0) throws IOException {
        throw new AssertionError();
    }

    public void setDateHeader(String arg0, long arg1) {
        throw new AssertionError();
    }

    public void setHeader(String arg0, String arg1) {
        throw new AssertionError();
    }

    public void setIntHeader(String arg0, int arg1) {
        throw new AssertionError();
    }

    public void setStatus(int arg0) {
        throw new AssertionError();
    }

    public void setStatus(int arg0, String arg1) {
        throw new AssertionError();
    }

    public void flushBuffer() throws IOException {
        throw new AssertionError();
    }

    public int getBufferSize() {
        throw new AssertionError();
    }

    public String getCharacterEncoding() {
        throw new AssertionError();
    }

    public String getContentType() {
        throw new AssertionError();
    }

    public Locale getLocale() {
        throw new AssertionError();
    }

    public ServletOutputStream getOutputStream() throws IOException {
        throw new AssertionError();
    }

    public PrintWriter getWriter() throws IOException {
        throw new AssertionError();
    }

    public boolean isCommitted() {
        throw new AssertionError();
    }

    public void reset() {
        throw new AssertionError();
    }

    public void resetBuffer() {
        throw new AssertionError();
    }

    public void setBufferSize(int arg0) {
        throw new AssertionError();
    }

    public void setCharacterEncoding(String arg0) {
        throw new AssertionError();
    }

    public void setContentLength(int arg0) {
        throw new AssertionError();
    }

    public void setContentType(String arg0) {
        throw new AssertionError();
    }

    public void setLocale(Locale arg0) {
        throw new AssertionError();
    }

}
