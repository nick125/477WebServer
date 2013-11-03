/*
 * ConnectionHandler.java
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

package server;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.Protocol;
import protocol.ProtocolException;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * This class is responsible for handling a incoming request
 * by creating a {@link HttpRequest} object and sending the appropriate
 * response be creating a {@link HttpResponse} object. It implements
 * {@link Runnable} to be used in multi-threaded environment.
 *
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class ConnectionHandler implements Runnable {
    private Server server;
    private Socket socket;

    public ConnectionHandler(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    /**
     * @return the socket
     */
    public Socket getSocket() {
        return socket;
    }


    /**
     * The entry point for connection handler. It first parses
     * incoming request and creates a {@link HttpRequest} object,
     * then it creates an appropriate {@link HttpResponse} object
     * and sends the response back to the client (web browser).
     */
    public void run() {
        // Get the request start time
        long start = System.currentTimeMillis();

        InputStream inStream;
        OutputStream outStream;

        try {
            inStream = this.socket.getInputStream();
            outStream = this.socket.getOutputStream();
        } catch (Exception e) {
            // Cannot do anything if we have exception reading input or output stream
            e.printStackTrace();

            incrementCounter(start);
            return;
        }

        // At this point we have the input and output stream of the socket
        HttpRequest request = null;
        HttpResponse response = null;
        try {
            request = HttpRequest.read(inStream);
        } catch (ProtocolException pe) {
            // We have some sort of protocol exception. Get its status code and create response
            // We know only two kind of exception is possible inside fromInputStream
            // Protocol.BAD_REQUEST_CODE and Protocol.NOT_SUPPORTED_CODE
            int status = pe.getStatus();

            switch (status) {
                case Protocol.NOT_SUPPORTED_CODE:
                    writeResponse(start, outStream, HttpResponse.create505NotSupported(Protocol.CLOSE));
                    break;
                case Protocol.BAD_REQUEST_CODE:
                default:
                    writeResponse(start, outStream, HttpResponse.create400BadRequest(Protocol.CLOSE));
                    break;
            }

            return;
        } catch (Exception e) {
            e.printStackTrace();

            // For any other error, we will create bad request response as well
            writeResponse(start, outStream, HttpResponse.create400BadRequest(Protocol.CLOSE));
            return;
        }

        try {
            // Check if the protocol is acceptable
            if (!request.getVersion().equalsIgnoreCase(Protocol.VERSION)) {
                writeResponse(start, outStream, HttpResponse.create505NotSupported(Protocol.CLOSE));
                return;
            }

            switch (request.getMethod()) {
                case GET:
                    // TODO: Do something
                    break;
                case POST:
                    // TODO: Do something
                    break;
                case HEAD:
                    // TODO: do something
                    break;
                case DELETE:
                    // TODO: do something
                    break;
                case PUT:
                    // TODO: do something
                    break;
                default:
                    response = HttpResponse.create400BadRequest(Protocol.CLOSE);
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
            response = HttpResponse.create500InternalServerError(Protocol.CLOSE);
        }

        writeResponse(start, outStream, response);
    }

    private void writeResponse(long start, OutputStream outStream, HttpResponse response) {
        if (response != null) {
            try {
                response.write(outStream);
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            incrementCounter(start);
        }
    }

    private void incrementCounter(long start) {
        server.incrementConnections(1);

        // Get the end time
        long end = System.currentTimeMillis();
        this.server.incrementServiceTime(end - start);
    }
}
