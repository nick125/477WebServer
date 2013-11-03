/*
 * HttpRequest.java
 * Oct 7, 2012
 *
 * Simple Web Server (SWS) for CSSE 477
 * 
 * Copyright (C) 2012 Chandan Raj Rupakheti
 * 
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either 
 * version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/lgpl.html>.
 * 
 */


package protocol;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Represents a request object for HTTP.
 *
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class HttpRequest {
    private HttpMethod method;
    private String uri;
    private String version;
    private Map<String, String> header;
    private String body;

    private Map<String, String> queryParameters;

    private HttpRequest() {
        this.header = new CaseInsensitiveMap();
        body = "";
    }

    /**
     * The request method.
     *
     * @return the method
     */
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * The URI of the request object.
     *
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * The version of the http request.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * The key to value mapping in the request header fields.
     *
     * @return the header
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(header);
    }

    public String getHeader(String key) {
        return header.get(key);
    }

    /**
     * Reads raw data from the supplied input stream and constructs a
     * <tt>HttpRequest</tt> object out of the raw data.
     *
     * @param inputStream The input stream to read from.
     * @return A <tt>HttpRequest</tt> object.
     * @throws Exception Throws either {@link ProtocolException} for bad request or
     *                   {@link java.io.IOException} for socket input stream read errors.
     */
    public static HttpRequest read(InputStream inputStream) throws Exception {
        HttpRequest request = new HttpRequest();

        InputStreamReader inStreamReader = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(inStreamReader);

        //First Request Line: GET /somedir/page.html HTTP/1.1
        String line = reader.readLine();

        if (line == null) {
            throw new ProtocolException(Protocol.BAD_REQUEST_CODE, Protocol.BAD_REQUEST_TEXT);
        }

        StringTokenizer tokenizer = new StringTokenizer(line, " ");

        if (tokenizer.countTokens() != 3) {
            throw new ProtocolException(Protocol.BAD_REQUEST_CODE, Protocol.BAD_REQUEST_TEXT);
        }

        String method = tokenizer.nextToken();

        try {
            request.method = HttpMethod.valueOf(method);
        } catch (IllegalArgumentException e) {
            throw new ProtocolException(Protocol.NOT_IMPLEMENTED_CODE, Protocol.NOT_IMPLEMENTED_TEXT);
        }

        request.uri = tokenizer.nextToken();
        request.version = tokenizer.nextToken();

        line = reader.readLine().trim();

        while (!line.equals("")) {
            line = line.trim();
            int index = line.indexOf(' ');
            if (index > 0 && index < line.length() - 1) {
                String key = line.substring(0, index);
                String value = line.substring(index + 1);
                key = key.trim().toLowerCase();
                key = key.substring(0, key.length() - 1);    // remove ":" from the key
                value = value.trim();
                request.header.put(key, value);
            }
            line = reader.readLine().trim();
        }

        while (reader.ready()) {
            request.body += (char) reader.read();
        }

        GetParameters(request);

        return request;
    }

    private static void GetParameters(HttpRequest request) {
        // We should have the method, so check if this is a GET request
        if (request.getMethod() == HttpMethod.GET || request.getMethod() == HttpMethod.HEAD) {
            // Check if we have any query parameters
            if (!request.getUri().contains("?"))
                return;

            request.uri = request.uri.split("(.*)\\?(.*)?")[0];
            request.queryParameters = parseFormEncodedString(request.uri.split("(.*)\\?(.*)?")[1]);
        } else {
            // Check if the content-type of the body is application/x-www-form-urlencoded
            if (!request.getHeader("Content-Type").equalsIgnoreCase("application/x-www-form-urlencoded"))
                return;

            request.queryParameters = parseFormEncodedString(request.body);
        }
    }

    /**
     * @param body
     * @return
     */
    private static Map<String, String> parseFormEncodedString(String body) {
        int idx = body.indexOf('?');
        if (idx >= 0) {
            body = body.substring(idx + 1);
        }

        StringTokenizer tokenizer = new StringTokenizer(body, "&");
        HashMap<String, String> map = new HashMap<String, String>();

        while (tokenizer.hasMoreElements()) {
            String pair = tokenizer.nextToken();

            // See if we can split the URL into key/value
            int index = pair.indexOf('=');
            if (index == -1) {
                // Just have the key...
                map.put(pair, "");
                continue;
            }

            // Split the key and value
            String key = pair.substring(0, index);
            String value = URLDecoder.decode(pair.substring(index + 1));
            map.put(key, value);
        }
        return map;
    }


    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("----------------------------------\n");
        buffer.append(this.method);
        buffer.append(Protocol.SPACE);
        buffer.append(this.uri);
        buffer.append(Protocol.SPACE);
        buffer.append(this.version);
        buffer.append(Protocol.LF);

        for (Map.Entry<String, String> entry : this.header.entrySet()) {
            buffer.append(entry.getKey());
            buffer.append(Protocol.SEPARATOR);
            buffer.append(Protocol.SPACE);
            buffer.append(entry.getValue());
            buffer.append(Protocol.LF);
        }
        buffer.append("----------------------------------\n");
        return buffer.toString();
    }

    /**
     * @return
     */
    public String getBody() {
        return body;
    }
}
