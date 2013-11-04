/*
 * HttpResponse.java
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

import java.io.*;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.*;

/**
 * Represents a response object for HTTP.
 *
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class HttpResponse {
    private HttpResponseType type;
    private String version;
    private Map<String, String> headers;

    private File file;
    private byte[] body;

    private static final String DEFAULT_VERSION = Protocol.VERSION;

    public HttpResponse(HttpResponseType type) {
        this(DEFAULT_VERSION, type);
    }

    /**
     * Constructs a HttpResponse object using the supplied parameters, no headers, and no file
     *
     * @param version The HTTP Version (e.g., 1.0 or 1.1)
     * @param type    The response type (e.g., HttpResponseType.OK)
     */
    public HttpResponse(String version, HttpResponseType type) {
        this(version, type, new HashMap<String, String>(), null);
    }

    /**
     * Constructs a HttpResponse object using the supplied parameters, no headers, and no file
     *
     * @param version The HTTP Version (e.g., 1.0 or 1.1)
     * @param type    The response type (e.g., HttpResponseType.OK)
     * @param file    The file to be sent.
     */
    public HttpResponse(String version, HttpResponseType type, File file) {
        this(version, type, new HashMap<String, String>(), file);
    }

    /**
     * Constructs a HttpResponse object using the supplied parameters, no headers, and no file
     *
     * @param version The HTTP Version (e.g., 1.0 or 1.1)
     * @param type    The response type (e.g., HttpResponseType.OK)
     * @param headers The headers field map.
     * @param file    The file to be sent.
     */
    public HttpResponse(String version, HttpResponseType type, Map<String, String> headers, File file) {
        this.version = version;
        this.type = type;
        this.headers = headers;
        this.file = file;
    }

    /**
     * Gets the version of the HTTP request.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    public HttpResponseType getType() {
        return type;
    }

    /**
     * The file to be sent.
     *
     * @return the file
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns the headers fields associated with the response object.
     *
     * @return the headers
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public void putETag(String tag){
        headers.put("ETag", tag);
    }

    /**
     * Maps a key to value in the headers map.
     *
     * @param key   A key, e.g. "Host"
     * @param value A value, e.g. "www.rose-hulman.edu"
     */
    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    /**
     * Writes the data of the http response object to the output stream.
     *
     * @param outStream The output stream
     * @throws Exception
     */
    public void write(OutputStream outStream) throws Exception {
        BufferedOutputStream out = new BufferedOutputStream(outStream, Protocol.CHUNK_LENGTH);

        // First status line
        String line = String.format("%s %s%s", this.version, this.type.getFullType(), Protocol.CRLF);
        out.write(line.getBytes());

        // Write headers fields if there is something to write in headers field
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Write each headers field line
                out.write(String.format("%s: %s%s", key, value, Protocol.CRLF).getBytes());
            }
        }
        out.write(Protocol.CRLF.getBytes());

        // We are reading a file
        if (file != null) {
            FileInputStream fileInStream = new FileInputStream(file);
            BufferedInputStream inStream = new BufferedInputStream(fileInStream, Protocol.CHUNK_LENGTH);

            byte[] buffer = new byte[Protocol.CHUNK_LENGTH];
            int bytesRead;

            while ((bytesRead = inStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            inStream.close();
        } else if (body != null && body.length > 0) {
            out.write(body, 0, body.length);
        }

        out.flush();
    }

    /**
     * Convenience method for adding general headers to the supplied response object.
     *
     * @param response   The {@link HttpResponse} object whose headers needs to be filled in.
     * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
     */
    private static void fillGeneralHeader(HttpResponse response, String connection) {
        response.addHeader(Protocol.CONNECTION, connection);
        Date date = Calendar.getInstance().getTime();
        response.addHeader(Protocol.DATE, date.toString());
        File f = new File(connection);
        response.addHeader(Protocol.Server, Protocol.getServerInfo());
        response.addHeader(Protocol.PROVIDER, Protocol.AUTHOR);
    }

    public static HttpResponse createResponse(HttpResponseType type, String connection, File file) {
        HttpResponse response = new HttpResponse(DEFAULT_VERSION, type, file);
        fillGeneralHeader(response, connection);

        long timeSinceEpoch = file.lastModified();
        Date modifiedTime = new Date(timeSinceEpoch);
        response.addHeader(Protocol.LAST_MODIFIED, modifiedTime.toString());

        long length = file.length();
        response.addHeader(Protocol.CONTENT_LENGTH, String.format("%d", length));

        // Lets get MIME type for the file
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String mime = fileNameMap.getContentTypeFor(file.getName());
        // The fileNameMap cannot find mime type for all of the documents, e.g. doc, odt, etc.
        // So we will not add this field if we cannot figure out what a mime type is for the file.
        // Let browser do this job by itself.
        if (mime != null) {
            response.addHeader(Protocol.CONTENT_TYPE, mime);
        }

        return response;
    }

    public static HttpResponse createResponse(HttpResponseType type, String connection, String body) {
        HttpResponse response = new HttpResponse(DEFAULT_VERSION, type);
        fillGeneralHeader(response, connection);
        response.body = body.getBytes();

        response.addHeader(Protocol.CONTENT_LENGTH, String.format("%d", response.body.length));

        return response;
    }

    public static HttpResponse createResponse(HttpResponseType type, String connection) {
        HttpResponse response = new HttpResponse(DEFAULT_VERSION, type);
        fillGeneralHeader(response, connection);
        return response;
    }

    /**
     * Creates a {@link HttpResponse} object for sending the supplied file with supplied connection
     * parameter.
     *
     * @param file       The {@link File} to be sent.
     * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
     * @return A {@link HttpResponse} object represent 200 status.
     */
    public static HttpResponse create200OK(File file, String connection) {
        return createResponse(HttpResponseType.OK, connection, file);
    }

    /**
     * Creates a {@link HttpResponse} object for sending bad request response.
     *
     * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
     * @return A {@link HttpResponse} object represent 400 status.
     */
    public static HttpResponse create400BadRequest(String connection) {
        return createResponse(HttpResponseType.BadRequest, connection);
    }

    /**
     * Creates a {@link HttpResponse} object for sending not found response.
     *
     * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
     * @return A {@link HttpResponse} object represent 404 status.
     */
    public static HttpResponse create404NotFound(String connection) {
        return createResponse(HttpResponseType.NotFound, connection, "404 - Page Not Found!");
    }

    /**
     * Creates a {@link HttpResponse} object for sending version not supported response.
     *
     * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
     * @return A {@link HttpResponse} object represent 505 status.
     */
    public static HttpResponse create505NotSupported(String connection) {
        return createResponse(HttpResponseType.NotSupported, connection);

    }

    /**
     * Creates a {@link HttpResponse} object for sending file not modified response.
     *
     * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
     * @return A {@link HttpResponse} object represent 304 status.
     */
    public static HttpResponse create304NotModified(String connection) {
        return createResponse(HttpResponseType.NotModified, connection);
    }

    /**
     * Creates a new 301 Moved Permanently response
     *
     * @param connection
     * @return A {@link HttpResponse} for a 301 Moved Permanently response
     */
    public static HttpResponse create301MovedPermanently(String connection) {
        return createResponse(HttpResponseType.MovedPermanently, connection);
    }

    /**
     * Creates a new 201 Created response
     *
     * @param connection
     * @return A {@link HttpResponse} for a 201 Created response
     */
    public static HttpResponse create201Created(String connection) {
        return createResponse(HttpResponseType.Created, connection);
    }

    public static HttpResponse create500InternalServerError(String connection) {
        return createResponse(HttpResponseType.InternalServerError, connection);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        OutputStream outBuffer = new ByteArrayOutputStream();
        buffer.append("----------------------------------\n");

        try {
            write(outBuffer);
            buffer.append(outBuffer.toString());
        } catch (Exception e) {
            buffer.append("Got error building request:");
            e.printStackTrace();
        }

        return buffer.toString();
    }


}
