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

import plugin.PluginLoader;
import pluginAPI.IRequestHandler;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;

/**
 * This represents a welcoming server for the incoming
 * TCP request from a HTTP client such as a web browser.
 *
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class Server implements Runnable {
    private final int numberCores;
    private final ArrayList<Thread> threads;

    private final ConcurrentLinkedQueue<Socket> socketQueue;

    private int port;
    private boolean stop;
    private ServerSocket socket;

    private List<IRequestHandler> requestHandlers;

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

        this.numberCores = Runtime.getRuntime().availableProcessors();
        this.threads = new ArrayList<Thread>();
        this.socketQueue = new ConcurrentLinkedQueue<Socket>();

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

    public List<IRequestHandler> getRequestHandlers() {
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
            return 0;
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
        // Create the thread pool threads
        final Server server = this;

        for (int threadID = 0; threadID < numberCores; threadID++)
        {
            Thread newThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isRunning()) {
                        // Check the socket queue
                        Socket queuedSocket = socketQueue.poll();
                        if (queuedSocket == null) continue;

                        // Start a connection handler
                        ConnectionHandler handler = new ConnectionHandler(server, queuedSocket);
                        handler.run();
                    }
                }
            });

            newThread.start();

            threads.add(threadID, newThread);
        }

        try {
            this.socket = new ServerSocket(port);

            // Now keep welcoming new connections until stop flag is set to true
            while (!this.stop) {
                // Listen for incoming socket connection
                // This method block until somebody makes a request
                Socket connectionSocket = this.socket.accept();

                // Queue the connection
                socketQueue.add(connectionSocket);
            }

            this.socket.close();
        } catch (Exception e) {
            e.printStackTrace();

            this.stop = true;
        }
    }

    public boolean isRunning() {
        return (!this.stop && (this.socket == null || !this.socket.isClosed()));
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
            System.out.println("Error while trying to stop server");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int port = 8000;

        if (args.length > 0)
            port = Integer.parseInt(args[0]);

        // Setup the plugin loader
        PluginLoader<IRequestHandler> pluginLoader = new PluginLoader<IRequestHandler>();

        // Start the listen loop
        final Server server = new Server(port);
        Thread serverThread = new Thread(server);
        serverThread.start();

        // Setup a ctrl-c handler
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                server.stop();
            }
        });

        // Now, we can loop until ctrl-c
        while (server.isRunning()) {
            // Check for new plugins
            server.requestHandlers = pluginLoader.getPlugins(IRequestHandler.class);

            // Print statistics
            System.out.println(String.format("Service Rate: %.2f", server.getServiceRate()));

            // Sleep for a little bit
            Thread.sleep(5000);
        }
    }
}
