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
    private String version;
    private int status;
    private String statusText;
    private Map<String, String> header;
    private File file;


    /**
     * Constructs a HttpResponse object using the supplied parameters, no headers, and no file
     *
     * @param version The HTTP Version (e.g., 1.0 or 1.1)
     * @param status  The integer response type (e.g., 200)
     * @param phrase  The text response type (e.g., "OK")
     */
    public HttpResponse(String version, int status, String phrase) {
        this(version, status, phrase, new HashMap<String, String>(), null);
    }

    /**
     * Constructs a HttpResponse object using supplied parameter
     *
     * @param version The http version.
     * @param status  The response status.
     * @param phrase  The response status statusText.
     * @param header  The header field map.
     * @param file    The file to be sent.
     */
    public HttpResponse(String version, int status, String phrase, Map<String, String> header, File file) {
        this.version = version;
        this.status = status;
        this.statusText = phrase;
        this.header = header;
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

    /**
     * Gets the status code of the response object.
     *
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    /**
     * Gets the status statusText of the response object.
     *
     * @return the statusText
     */
    public String getStatusText() {
        return statusText;
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
     * Returns the header fields associated with the response object.
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
     * Maps a key to value in the header map.
     *
     * @param key   A key, e.g. "Host"
     * @param value A value, e.g. "www.rose-hulman.edu"
     */
    public void addHeader(String key, String value) {
        this.header.put(key, value);
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
        String line = this.version + Protocol.SPACE + this.status + Protocol.SPACE + this.statusText + Protocol.CRLF;
        out.write(line.getBytes());

        // Write header fields if there is something to write in header field
        if (header != null && !header.isEmpty()) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Write each header field line
                line = key + Protocol.SEPERATOR + Protocol.SPACE + value + Protocol.CRLF;
                out.write(line.getBytes());
            }
        }
        out.write(Protocol.CRLF.getBytes());

        // We are reading a file
        if (this.getStatus() == Protocol.OK_CODE && file != null) {
            FileInputStream fileInStream = new FileInputStream(file);
            BufferedInputStream inStream = new BufferedInputStream(fileInStream, Protocol.CHUNK_LENGTH);

            byte[] buffer = new byte[Protocol.CHUNK_LENGTH];
            int bytesRead;

            while ((bytesRead = inStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            inStream.close();
        }
        out.flush();
    }

    /**
     * Convenience method for adding general header to the supplied response object.
     *
     * @param response   The {@link HttpResponse} object whose header needs to be filled in.
     * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
     */
    private static void fillGeneralHeader(HttpResponse response, String connection) {
        response.addHeader(Protocol.CONNECTION, connection);
        Date date = Calendar.getInstance().getTime();
        response.addHeader(Protocol.DATE, date.toString());
        response.addHeader(Protocol.Server, Protocol.getServerInfo());
        response.addHeader(Protocol.PROVIDER, Protocol.AUTHOR);
    }

    /**
     * Creates a {@link HttpResponse} object for sending the supplied file with supplied connection
     * parameter.
     *
     * @param file       The {@link File} to be sent.
     * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
     * @return A {@link HttpResponse} object represent 200 status.
     */
    public static HttpResponse create200OK(File file, String connection, boolean get) {
        HttpResponse response = new HttpResponse(Protocol.VERSION, Protocol.OK_CODE,
                Protocol.OK_TEXT, new HashMap<String, String>(), get ? file : null);

        fillGeneralHeader(response, connection);
        long timeSinceEpoch = file.lastModified();
        Date modifiedTime = new Date(timeSinceEpoch);
        response.addHeader(Protocol.LAST_MODIFIED, modifiedTime.toString());

        long length = file.length();
        response.addHeader(Protocol.CONTENT_LENGTH, length + "");

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

    /**
     * Creates a {@link HttpResponse} object for sending bad request response.
     *
     * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
     * @return A {@link HttpResponse} object represent 400 status.
     */
    public static HttpResponse create400BadRequest(String connection) {
        HttpResponse response = new HttpResponse(Protocol.VERSION, Protocol.BAD_REQUEST_CODE,
                Protocol.BAD_REQUEST_TEXT);
        fillGeneralHeader(response, connection);
        return response;
    }

    /**
     * Creates a {@link HttpResponse} object for sending not found response.
     *
     * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
     * @return A {@link HttpResponse} object represent 404 status.
     */
    public static HttpResponse create404NotFound(String connection) {
        HttpResponse response = new HttpResponse(Protocol.VERSION, Protocol.NOT_FOUND_CODE,
                Protocol.NOT_FOUND_TEXT);
        fillGeneralHeader(response, connection);
        return response;
    }

    /**
     * Creates a {@link HttpResponse} object for sending version not supported response.
     *
     * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
     * @return A {@link HttpResponse} object represent 505 status.
     */
    public static HttpResponse create505NotSupported(String connection) {
        HttpResponse response = new HttpResponse(Protocol.VERSION, Protocol.NOT_SUPPORTED_CODE,
                Protocol.NOT_SUPPORTED_TEXT);
        fillGeneralHeader(response, connection);
        return response;
    }

    /**
     * Creates a {@link HttpResponse} object for sending file not modified response.
     *
     * @param connection Supported values are {@link Protocol#OPEN} and {@link Protocol#CLOSE}.
     * @return A {@link HttpResponse} object represent 304 status.
     */
    public static HttpResponse create304NotModified(String connection) {
        HttpResponse response = new HttpResponse(Protocol.VERSION, Protocol.NOT_MODIFIED_CODE,
                Protocol.NOT_MODIFIED_TEXT);
        fillGeneralHeader(response, connection);
        return response;
    }

    /**
     * Creates a new 301 Moved Permanently response
     *
     * @param connection
     * @return A {@link HttpResponse} for a 301 Moved Permanently response
     */
    public static HttpResponse create301MovedPermanently(String connection) {
        HttpResponse response = new HttpResponse(Protocol.VERSION, Protocol.MOVED_PERMANENTLY_CODE,
                Protocol.MOVED_PERMANENTLY_TEXT);
        fillGeneralHeader(response, connection);
        return response;
    }

    /**
     * Creates a new 201 Created response
     *
     * @param connection
     * @return A {@link HttpResponse} for a 201 Created response
     */
    public static HttpResponse create201Created(String connection) {
        HttpResponse response = new HttpResponse(Protocol.VERSION, Protocol.CREATED_CODE,
                Protocol.CREATED_TEXT);
        fillGeneralHeader(response, connection);
        return response;
    }

    public static HttpResponse create500InternalServerError(String connection) {
        HttpResponse response = new HttpResponse(Protocol.VERSION, Protocol.INTERNAL_ERROR_CODE,
                Protocol.INTERNAL_ERROR_TEXT);
        fillGeneralHeader(response, connection);
        return response;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("----------------------------------\n");
        buffer.append(this.version);
        buffer.append(Protocol.SPACE);
        buffer.append(this.status);
        buffer.append(Protocol.SPACE);
        buffer.append(this.statusText);
        buffer.append(Protocol.LF);

        for (Map.Entry<String, String> entry : this.header.entrySet()) {
            buffer.append(entry.getKey());
            buffer.append(Protocol.SEPERATOR);
            buffer.append(Protocol.SPACE);
            buffer.append(entry.getValue());
            buffer.append(Protocol.LF);
        }

        buffer.append(Protocol.LF);
        if (file != null) {
            buffer.append("Data: ");
            buffer.append(this.file.getAbsolutePath());
        }
        buffer.append("\n----------------------------------\n");
        return buffer.toString();
    }


}
