package org.kohsuke.stapler;

import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import java.io.IOException;
import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.text.ParseException;

/**
 * @author Kohsuke Kawaguchi
 */
class RequestImpl extends HttpServletRequestWrapper implements StaplerRequest {
    /**
     * Tokenized URLs and consumed tokens.
     * This object is modified by {@link Stapler} as we parse through the URL.
     */
    public final TokenList tokens;
    /**
     * Ancesotr nodes traversed so far.
     * This object is modified by {@link Stapler} as we parse through the URL.
     */
    public final List<AncestorImpl> ancestors;

    private final Stapler stapler;

    // lazily computed
    private String rest;

    private final String originalRequestURI;

    public RequestImpl(Stapler stapler, HttpServletRequest request, List<AncestorImpl> ancestors, TokenList tokens) {
        super(request);
        this.stapler = stapler;
        this.ancestors = ancestors;
        this.tokens = tokens;
        this.originalRequestURI = request.getRequestURI();
    }

    public String getRestOfPath() {
        if(rest==null)
            rest = assembleRestOfPath(tokens);
        return rest;
    }

    public ServletContext getServletContext() {
        return stapler.getServletContext();
    }

    private static String assembleRestOfPath(TokenList tokens) {
        StringBuffer buf = new StringBuffer();
        for( int i=tokens.idx; i<tokens.length(); i++ ) {
            buf.append('/');
            buf.append(tokens.get(i));
        }
        return buf.toString();
    }

    public RequestDispatcher getView(Object it,String jspName) throws IOException {
        return stapler.getResourceDispatcher(it,jspName);
    }

    public String getRootPath() {
        StringBuffer buf = super.getRequestURL();
        int idx = 0;
        for( int i=0; i<3; i++ )
            idx = buf.substring(idx).indexOf("/")+1;
        buf.setLength(idx-1);
        buf.append(super.getContextPath());
        return buf.toString();
    }

    public List getAncestors() {
        return ancestors;
    }

    public String getOriginalRequestURI() {
        return originalRequestURI;
    }

    public boolean checkIfModified(long lastModified, StaplerResponse rsp) {
        if(lastModified<=0)
            return false;

        // send out Last-Modified, or check If-Modified-Since
        String since = getHeader("If-Modified-Since");
        if(since!=null) {
            try {
                long ims = Stapler.HTTP_DATE_FORMAT.parse(since).getTime();
                if(lastModified<ims+1000) {
                    // +1000 because date header is second-precision and Java has milli-second precision
                    rsp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    return true;
                }
            } catch (ParseException e) {
                // just ignore and serve the content
            }
        }
        rsp.setHeader("Last-Modified",Stapler.HTTP_DATE_FORMAT.format(new Date(lastModified)));
        return false;
    }

    public boolean checkIfModified(Date timestampOfResource, StaplerResponse rsp) {
        return checkIfModified(timestampOfResource.getTime(),rsp);
    }

    public boolean checkIfModified(Calendar timestampOfResource, StaplerResponse rsp) {
        return checkIfModified(timestampOfResource.getTimeInMillis(),rsp);
    }
}