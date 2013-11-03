/*
 * Server.java
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

import plugins.IRequestHandler;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * This represents a welcoming server for the incoming
 * TCP request from a HTTP client such as a web browser.
 *
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class Server implements Runnable {
    private int port;
    private boolean stop;
    private ServerSocket socket;

    private ArrayList<IRequestHandler> requestHandlers;

    private long connections;
    private long serviceTime;

    /**
     * @param port
     */
    public Server(int port) {
        this.port = port;
        this.stop = false;
        this.connections = 0;
        this.serviceTime = 0;

        this.requestHandlers = new ArrayList<IRequestHandler>();
    }

    /**
     * Gets the port number for this web server.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    public ArrayList<IRequestHandler> getRequestHandlers()
    {
        return this.requestHandlers;
    }

    /**
     * Returns connections serviced per second.
     * Synchronized to be used in threaded environment.
     *
     * @return
     */
    public synchronized double getServiceRate() {
        if (this.serviceTime == 0) {
            return Long.MIN_VALUE;
        }

        double rate = this.connections / (double) this.serviceTime;
        rate = rate * 1000;
        return rate;
    }

    /**
     * Increments number of connection by the supplied value.
     * Synchronized to be used in threaded environment.
     *
     * @param value
     */
    public synchronized void incrementConnections(long value) {
        this.connections += value;
    }

    /**
     * Increments the service time by the supplied value.
     * Synchronized to be used in threaded environment.
     *
     * @param value
     */
    public synchronized void incrementServiceTime(long value) {
        this.serviceTime += value;
    }

    /**
     * The entry method for the main server thread that accepts incoming
     * TCP connection request and creates a {@link ConnectionHandler} for
     * the request.
     */
    public void run() {
        try {
            this.socket = new ServerSocket(port);

            // Now keep welcoming new connections until stop flag is set to true
            while (!this.stop) {
                // Listen for incoming socket connection
                // This method block until somebody makes a request
                Socket connectionSocket = this.socket.accept();

                // Create a handler for this incoming connection and start the handler in a new thread
                ConnectionHandler handler = new ConnectionHandler(this, connectionSocket);
                new Thread(handler).start();
            }

            this.socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stops the server from listening further.
     */
    public synchronized void stop() {
        if (this.stop)
            return;

        this.stop = true;
        try {
            // This will force socket to come out of the blocked accept() method
            // in the main loop of the start() method
            Socket socket = new Socket(InetAddress.getLocalHost(), port);

            // We do not have any other job for this socket so just close it
            socket.close();
        } catch (Exception e) {
        }
    }
}
