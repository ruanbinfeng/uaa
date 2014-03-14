/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cloudfoundry.identity.uaa.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Modified implementation of the RequestDumperFilter(Valve) from
 * Apache Tomcat that only reports data, as a single entry,
 * when a session has been created.
 * 
 * @author Filip Hanik
 * @author Craig R. McClanahan
 */

public class SessionLeakTracker implements Filter {
    public static String SESSION_LEAK_TRACKER = "session.leak.track";

    private static volatile boolean trackerEnabled = true;

    private static final String NON_HTTP_REQ_MSG = "Not available. Non-http request.";
    private static final String NON_HTTP_RES_MSG = "Not available. Non-http response.";

    private static final ThreadLocal<Timestamp> timestamp = new ThreadLocal<Timestamp>() {
        @Override
        protected Timestamp initialValue() {
            return new Timestamp();
        }
    };

    private static final ThreadLocal<StringBuilder> logTracker = new ThreadLocal<StringBuilder>() {
        @Override
        protected StringBuilder initialValue() {
            return new StringBuilder();
        }
    };

    /**
     * The logger for this class.
     */
    private static final Log log = LogFactory.getLog(SessionLeakTracker.class);

    /**
     * Log the interesting request parameters, invoke the next Filter in the
     * sequence, and log the interesting response parameters.
     * 
     * @param request
     *            The servlet request to be processed
     * @param response
     *            The servlet response to be created
     * @param chain
     *            The filter chain being processed
     * 
     * @exception IOException
     *                if an input/output error occurs
     * @exception ServletException
     *                if a servlet error occurs
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {

        String enabledParam = request.getParameter(SESSION_LEAK_TRACKER);
        if ("true".equals(enabledParam)) {
            SessionLeakTracker.trackerEnabled = true;
        } else if ("false".equals(enabledParam)) {
            SessionLeakTracker.trackerEnabled = false;
        }

        if (SessionLeakTracker.trackerEnabled) {
            trackRequest(new RequestWrapper((HttpServletRequest)request), (HttpServletResponse)response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    protected void trackRequest(HttpServletRequest hRequest, HttpServletResponse hResponse,
                    FilterChain chain) throws IOException, ServletException {
        boolean hasSession = false;
        boolean hadSession = hRequest.getSession(false)!=null;
        try {

                        // Log pre-service information
            doLog("START TIME        ", getTimestamp());

            doLog("        requestURI", hRequest.getRequestURI());
            doLog("          authType", hRequest.getAuthType());
            doLog("          User-Agent", hRequest.getHeader("user-agent"));

            doLog(" characterEncoding", hRequest.getCharacterEncoding());
            doLog("     contentLength", Integer.valueOf(hRequest.getContentLength()).toString());
            doLog("       contentType", hRequest.getContentType());

            doLog("       contextPath", hRequest.getContextPath());
            Cookie cookies[] = hRequest.getCookies();
            if (cookies != null) {
                for (int i = 0; i < cookies.length; i++) {
                    doLog("            cookie",
                                    cookies[i].getName() + "=" + cookies[i].getValue());
                }
            }
            Enumeration<String> hnames = hRequest.getHeaderNames();
            while (hnames.hasMoreElements()) {
                String hname = hnames.nextElement();
                Enumeration<String> hvalues = hRequest.getHeaders(hname);
                while (hvalues.hasMoreElements()) {
                    String hvalue = hvalues.nextElement();
                    doLog("            header", hname + "=" + hvalue);
                }
            }

            doLog("            locale", hRequest.getLocale().toString());
            doLog("            method", hRequest.getMethod());

            Enumeration<String> pnames = hRequest.getParameterNames();
            while (pnames.hasMoreElements()) {
                String pname = pnames.nextElement();
                String pvalues[] = hRequest.getParameterValues(pname);
                StringBuilder result = new StringBuilder(pname);
                result.append('=');
                for (int i = 0; i < pvalues.length; i++) {
                    if (i > 0) {
                        result.append(", ");
                    }
                    result.append(pvalues[i]);
                }
                doLog("         parameter", result.toString());
            }

            doLog("          pathInfo", hRequest.getPathInfo());
            doLog("          protocol", hRequest.getProtocol());
            doLog("       queryString", hRequest.getQueryString());
            doLog("        remoteAddr", hRequest.getRemoteAddr());
            doLog("        remoteHost", hRequest.getRemoteHost());

            doLog("        remoteUser", hRequest.getRemoteUser());
            doLog("requestedSessionId", hRequest.getRequestedSessionId());
            doLog("            scheme", hRequest.getScheme());
            doLog("        serverName", hRequest.getServerName());
            doLog("        serverPort", Integer.valueOf(hRequest.getServerPort()).toString());
            doLog("       servletPath", hRequest.getServletPath());

            doLog("          isSecure", Boolean.valueOf(hRequest.isSecure()).toString());
            doLog("        hadSession", Boolean.valueOf(hasSession).toString());
            doLog("------------------", "--------------------------------------------");

            
          
            // Perform the request
            chain.doFilter(hRequest, hResponse);

            // check if we created a session
            HttpSession session = hRequest.getSession(false);
            hasSession = session != null;
            doLog("        hasSession", Boolean.valueOf(hasSession).toString());
            

            // Log post-service information
            doLog("------------------", "--------------------------------------------");
            doLog("          authType", hRequest.getAuthType());
            doLog("       contentType", hResponse.getContentType());

            Iterable<String> rhnames = hResponse.getHeaderNames();
            for (String rhname : rhnames) {
                Iterable<String> rhvalues = hResponse.getHeaders(rhname);
                for (String rhvalue : rhvalues) {
                    doLog("            header", rhname + "=" + rhvalue);
                }
            }

            doLog("        remoteUser", hRequest.getRemoteUser());
            doLog("            status", Integer.valueOf(hResponse.getStatus()).toString());
            doLog("END TIME          ", getTimestamp());
            doLog("==================", "============================================");
        } finally {
            StringBuilder builder = logTracker.get();
            if (hasSession && (!hadSession)) {
                log.warn("\n" + builder.toString());
            }
            builder.delete(0, builder.length());
        }
    }

    private static void doLog(String attribute, String value) {
        StringBuilder sb = new StringBuilder(80);
        sb.append(attribute);
        sb.append('=');
        sb.append(value);
        sb.append("\n");
        logTracker.get().append(sb);
    }

    private String getTimestamp() {
        Timestamp ts = timestamp.get();
        long currentTime = System.currentTimeMillis();

        if ((ts.date.getTime() + 999) < currentTime) {
            ts.date.setTime(currentTime - (currentTime % 1000));
            ts.update();
        }
        return ts.dateString;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // NOOP
    }

    @Override
    public void destroy() {
        // NOOP
    }

    private static final class Timestamp {
        private final Date date = new Date(0);
        private final SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        private String dateString = format.format(date);

        private void update() {
            dateString = format.format(date);
        }
    }

    private static class RequestWrapper extends HttpServletRequestWrapper {

        public RequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public HttpSession getSession(boolean create) {
            HttpSession session = super.getSession(false);
            if (create && session == null) {
                Exception x = new Exception("Session Creation");
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                x.printStackTrace(pw);
                pw.flush();
                doLog("--------getSession", sw.toString());
            }
            return super.getSession(create);
        }

        @Override
        public HttpSession getSession() {
            return getSession(true);
        }

    }

}
