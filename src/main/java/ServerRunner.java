import servlet.CustomerServlet;
import servlet.DashboardServlet;
import servlet.RentalServlet;
import servlet.ReturnVehicleServlet;
import servlet.VehicleServlet;
import util.DatabaseConnection;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Embedded HTTP Application Launcher for Vehicle Rental Management System.
 * Serves frontend static assets (HTML, CSS, JS) and dispatches API routes directly to Servlets.
 */
public class ServerRunner {

    private static final int PORT = 8080;
    private static final File WEB_ROOT = new File("web");

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println("  VEHICLE RENTAL MANAGEMENT SYSTEM (KTU S3 OOP)  ");
        System.out.println("=================================================");
        System.out.println("[ServerRunner] Initializing database and HTTP server...");

        // Trigger Database init
        try {
            DatabaseConnection.getConnection().close();
        } catch (Exception e) {
            System.err.println("[ServerRunner] Database setup warning: " + e.getMessage());
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Servlet Adapters
        final DashboardServlet dashboardServlet = new DashboardServlet();
        final VehicleServlet vehicleServlet = new VehicleServlet();
        final CustomerServlet customerServlet = new CustomerServlet();
        final RentalServlet rentalServlet = new RentalServlet();
        final ReturnVehicleServlet returnServlet = new ReturnVehicleServlet();

        initServlet(dashboardServlet, "DashboardServlet");
        initServlet(vehicleServlet, "VehicleServlet");
        initServlet(customerServlet, "CustomerServlet");
        initServlet(rentalServlet, "RentalServlet");
        initServlet(returnServlet, "ReturnVehicleServlet");

        // API Endpoints Contexts
        server.createContext("/api/dashboard", new ServletHandler(dashboardServlet));
        server.createContext("/api/vehicles", new ServletHandler(vehicleServlet));
        server.createContext("/api/customers", new ServletHandler(customerServlet));
        server.createContext("/api/rentals", new ServletHandler(rentalServlet));
        server.createContext("/api/returns", new ServletHandler(returnServlet));

        // Static Files Context
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("\n[ServerRunner] Server started successfully!");
        System.out.println("[ServerRunner] Access the website at: http://localhost:" + PORT + "/");
        System.out.println("=================================================\n");
    }

    private static void initServlet(HttpServlet servlet, String name) {
        try {
            servlet.init(new DummyServletConfig(name));
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }

    // Adapt HttpServer exchange to Java Servlet Request/Response
    private static class ServletHandler implements HttpHandler {
        private final HttpServlet servlet;

        public ServletHandler(HttpServlet servlet) {
            this.servlet = servlet;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
            try (InputStream is = exchange.getRequestBody()) {
                byte[] buf = new byte[1024];
                int n;
                while ((n = is.read(buf)) != -1) {
                    bodyStream.write(buf, 0, n);
                }
            }
            byte[] requestBody = bodyStream.toByteArray();

            Map<String, String[]> parameterMap = new HashMap<>();
            String rawQuery = exchange.getRequestURI().getRawQuery();
            parseQueryParams(rawQuery, parameterMap);

            String contentType = getHeader(exchange, "Content-Type");
            if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
                String bodyStr = new String(requestBody, StandardCharsets.UTF_8);
                parseQueryParams(bodyStr, parameterMap);
            }

            ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
            WrapperResponse response = new WrapperResponse(responseBuffer);
            WrapperRequest request = new WrapperRequest(exchange, requestBody, parameterMap);

            try {
                servlet.service(request, response);
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(500);
                response.getWriter().write("{\"success\": false, \"message\": \"Internal Server Error: " + e.getMessage() + "\"}");
            }

            byte[] respBytes = responseBuffer.toByteArray();
            exchange.getResponseHeaders().set("Content-Type", response.getContentType() != null ? response.getContentType() : "application/json");
            exchange.sendResponseHeaders(response.getStatus(), respBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(respBytes);
            }
        }

        private String getHeader(HttpExchange exchange, String name) {
            List<String> list = exchange.getRequestHeaders().get(name);
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        }

        private void parseQueryParams(String query, Map<String, String[]> map) {
            if (query == null || query.trim().isEmpty()) return;
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                try {
                    String k = URLDecoder.decode(kv[0], "UTF-8");
                    String v = kv.length > 1 ? URLDecoder.decode(kv[1], "UTF-8") : "";
                    String[] existing = map.get(k);
                    if (existing == null) {
                        map.put(k, new String[]{v});
                    } else {
                        String[] next = Arrays.copyOf(existing, existing.length + 1);
                        next[existing.length] = v;
                        map.put(k, next);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            File targetFile = new File(WEB_ROOT, path);
            if (!targetFile.exists() || targetFile.isDirectory()) {
                // Fallback to index.html for client side routing if needed
                targetFile = new File(WEB_ROOT, "index.html");
            }

            if (!targetFile.exists()) {
                String notFound = "<h1>404 Not Found</h1>";
                exchange.sendResponseHeaders(404, notFound.length());
                exchange.getResponseBody().write(notFound.getBytes());
                exchange.close();
                return;
            }

            String mime = getMimeType(targetFile.getName());
            exchange.getResponseHeaders().set("Content-Type", mime);
            byte[] fileBytes = Files.readAllBytes(targetFile.toPath());
            exchange.sendResponseHeaders(200, fileBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }

        private String getMimeType(String fileName) {
            if (fileName.endsWith(".html") || fileName.endsWith(".htm")) return "text/html; charset=UTF-8";
            if (fileName.endsWith(".css")) return "text/css; charset=UTF-8";
            if (fileName.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (fileName.endsWith(".png")) return "image/png";
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
            if (fileName.endsWith(".svg")) return "image/svg+xml";
            if (fileName.endsWith(".json")) return "application/json";
            return "text/plain";
        }
    }

    // Lightweight Servlet Adapters for standalone runner
    private static class DummyServletConfig implements ServletConfig {
        private final String name;
        public DummyServletConfig(String name) { this.name = name; }
        public String getServletName() { return name; }
        public ServletContext getServletContext() { return null; }
        public String getInitParameter(String name) { return null; }
        public Enumeration<String> getInitParameterNames() { return Collections.emptyEnumeration(); }
    }

    private static class WrapperRequest extends javax.servlet.http.HttpServletRequestWrapper {
        private final HttpExchange exchange;
        private final byte[] body;
        private final Map<String, String[]> params;

        public WrapperRequest(HttpExchange exchange, byte[] body, Map<String, String[]> params) {
            super(new DummyHttpServletRequest());
            this.exchange = exchange;
            this.body = body;
            this.params = params;
        }

        @Override
        public String getMethod() { return exchange.getRequestMethod(); }
        @Override
        public String getRequestURI() { return exchange.getRequestURI().getPath(); }
        @Override
        public String getServletPath() { return exchange.getRequestURI().getPath(); }
        @Override
        public String getQueryString() { return exchange.getRequestURI().getRawQuery(); }
        @Override
        public String getContentType() {
            List<String> list = exchange.getRequestHeaders().get("Content-Type");
            return list != null && !list.isEmpty() ? list.get(0) : null;
        }
        @Override
        public String getParameter(String name) {
            String[] vals = params.get(name);
            return (vals != null && vals.length > 0) ? vals[0] : null;
        }
        @Override
        public Map<String, String[]> getParameterMap() { return params; }
        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8));
        }
        @Override
        public javax.servlet.ServletInputStream getInputStream() {
            final ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new javax.servlet.ServletInputStream() {
                public boolean isFinished() { return bais.available() == 0; }
                public boolean isReady() { return true; }
                public void setReadListener(javax.servlet.ReadListener readListener) {}
                public int read() { return bais.read(); }
            };
        }
    }

    private static class WrapperResponse extends javax.servlet.http.HttpServletResponseWrapper {
        private final ByteArrayOutputStream buffer;
        private PrintWriter writer;
        private int status = 200;
        private String contentType = "application/json";

        public WrapperResponse(ByteArrayOutputStream buffer) {
            super(new DummyHttpServletResponse());
            this.buffer = buffer;
        }

        @Override
        public void setStatus(int sc) { this.status = sc; }
        @Override
        public int getStatus() { return status; }
        @Override
        public void setContentType(String type) { this.contentType = type; }
        @Override
        public String getContentType() { return contentType; }
        @Override
        public PrintWriter getWriter() {
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(buffer, StandardCharsets.UTF_8), true);
            }
            return writer;
        }
        @Override
        public javax.servlet.ServletOutputStream getOutputStream() {
            return new javax.servlet.ServletOutputStream() {
                public boolean isReady() { return true; }
                public void setWriteListener(javax.servlet.WriteListener writeListener) {}
                public void write(int b) { buffer.write(b); }
            };
        }
    }

    // Dummy Servlet instances required for Wrapper inheritance
    private static class DummyHttpServletRequest implements HttpServletRequest {
        public Object getAttribute(String name) { return null; }
        public Enumeration<String> getAttributeNames() { return Collections.emptyEnumeration(); }
        public String getCharacterEncoding() { return "UTF-8"; }
        public void setCharacterEncoding(String env) {}
        public int getContentLength() { return 0; }
        public long getContentLengthLong() { return 0; }
        public String getContentType() { return null; }
        public javax.servlet.ServletInputStream getInputStream() { return null; }
        public String getParameter(String name) { return null; }
        public Enumeration<String> getParameterNames() { return Collections.emptyEnumeration(); }
        public String[] getParameterValues(String name) { return new String[0]; }
        public Map<String, String[]> getParameterMap() { return Collections.emptyMap(); }
        public String getProtocol() { return "HTTP/1.1"; }
        public String getScheme() { return "http"; }
        public String getServerName() { return "localhost"; }
        public int getServerPort() { return 8080; }
        public BufferedReader getReader() { return null; }
        public String getRemoteAddr() { return "127.0.0.1"; }
        public String getRemoteHost() { return "localhost"; }
        public void setAttribute(String name, Object o) {}
        public void removeAttribute(String name) {}
        public Locale getLocale() { return Locale.getDefault(); }
        public Enumeration<Locale> getLocales() { return Collections.emptyEnumeration(); }
        public boolean isSecure() { return false; }
        public javax.servlet.RequestDispatcher getRequestDispatcher(String path) { return null; }
        public String getRealPath(String path) { return null; }
        public int getRemotePort() { return 0; }
        public String getLocalName() { return "localhost"; }
        public String getLocalAddr() { return "127.0.0.1"; }
        public int getLocalPort() { return 8080; }
        public ServletContext getServletContext() { return null; }
        public javax.servlet.AsyncContext startAsync() { return null; }
        public javax.servlet.AsyncContext startAsync(javax.servlet.ServletRequest r, javax.servlet.ServletResponse p) { return null; }
        public boolean isAsyncStarted() { return false; }
        public boolean isAsyncSupported() { return false; }
        public javax.servlet.AsyncContext getAsyncContext() { return null; }
        public javax.servlet.DispatcherType getDispatcherType() { return null; }
        public String getAuthType() { return null; }
        public Cookie[] getCookies() { return new Cookie[0]; }
        public long getDateHeader(String name) { return 0; }
        public String getHeader(String name) { return null; }
        public Enumeration<String> getHeaders(String name) { return Collections.emptyEnumeration(); }
        public Enumeration<String> getHeaderNames() { return Collections.emptyEnumeration(); }
        public int getIntHeader(String name) { return 0; }
        public String getMethod() { return "GET"; }
        public String getPathInfo() { return null; }
        public String getPathTranslated() { return null; }
        public String getContextPath() { return ""; }
        public String getQueryString() { return null; }
        public String getRemoteUser() { return null; }
        public boolean isUserInRole(String role) { return false; }
        public java.security.Principal getUserPrincipal() { return null; }
        public String getRequestedSessionId() { return null; }
        public String getRequestURI() { return "/"; }
        public StringBuffer getRequestURL() { return new StringBuffer("http://localhost:8080/"); }
        public String getServletPath() { return "/"; }
        public javax.servlet.http.HttpSession getSession(boolean create) { return null; }
        public javax.servlet.http.HttpSession getSession() { return null; }
        public String changeSessionId() { return null; }
        public boolean isRequestedSessionIdValid() { return false; }
        public boolean isRequestedSessionIdFromCookie() { return false; }
        public boolean isRequestedSessionIdFromURL() { return false; }
        public boolean isRequestedSessionIdFromUrl() { return false; }
        public boolean authenticate(HttpServletResponse response) { return false; }
        public void login(String username, String password) {}
        public void logout() {}
        public Collection<javax.servlet.http.Part> getParts() { return Collections.emptyList(); }
        public javax.servlet.http.Part getPart(String name) { return null; }
        public <T extends javax.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }
    }

    private static class DummyHttpServletResponse implements HttpServletResponse {
        public void addCookie(Cookie cookie) {}
        public boolean containsHeader(String name) { return false; }
        public String encodeURL(String url) { return url; }
        public String encodeRedirectURL(String url) { return url; }
        public String encodeUrl(String url) { return url; }
        public String encodeRedirectUrl(String url) { return url; }
        public void sendError(int sc, String msg) {}
        public void sendError(int sc) {}
        public void sendRedirect(String location) {}
        public void setDateHeader(String name, long date) {}
        public void addDateHeader(String name, long date) {}
        public void setHeader(String name, String value) {}
        public void addHeader(String name, String value) {}
        public void setIntHeader(String name, int value) {}
        public void addIntHeader(String name, int value) {}
        public void setStatus(int sc) {}
        public void setStatus(int sc, String sm) {}
        public int getStatus() { return 200; }
        public String getHeader(String name) { return null; }
        public Collection<String> getHeaders(String name) { return Collections.emptyList(); }
        public Collection<String> getHeaderNames() { return Collections.emptyList(); }
        public String getCharacterEncoding() { return "UTF-8"; }
        public String getContentType() { return "application/json"; }
        public javax.servlet.ServletOutputStream getOutputStream() { return null; }
        public PrintWriter getWriter() { return null; }
        public void setCharacterEncoding(String charset) {}
        public void setContentLength(int len) {}
        public void setContentLengthLong(long len) {}
        public void setContentType(String type) {}
        public void setBufferSize(int size) {}
        public int getBufferSize() { return 0; }
        public void flushBuffer() {}
        public void resetBuffer() {}
        public boolean isCommitted() { return false; }
        public void reset() {}
        public void setLocale(Locale loc) {}
        public Locale getLocale() { return Locale.getDefault(); }
    }
}
