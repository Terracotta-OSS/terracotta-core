package weblogic.servlet.internal;

import javax.servlet.http.HttpServletResponse;

// NOTE: real implementation sometimes has final access, this is removed by instrumentation.
// Real implementation also isn't abstract
public abstract class ServletResponseImpl implements HttpServletResponse {

    protected ServletResponseImpl() {
        // real implementation has package-private
        // constructor, it is made protected at runtime
    }

}
