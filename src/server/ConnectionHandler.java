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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.Protocol;
import protocol.ProtocolException;

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
		// Get the start time
		long start = System.currentTimeMillis();
		
		InputStream inStream = null;
		OutputStream outStream = null;
		
		try {
			inStream = this.socket.getInputStream();
			outStream = this.socket.getOutputStream();
		}
		catch(Exception e) {
			// Cannot do anything if we have exception reading input or output stream
			e.printStackTrace();
			
			server.incrementConnections(1);
			long end = System.currentTimeMillis();
			this.server.incrementServiceTime(end-start);
			return;
		}
		
		// At this point we have the input and output stream of the socket
		HttpRequest request = null;
		HttpResponse response = null;
		try {
			request = HttpRequest.read(inStream);
//			System.out.println(request);
		}
		catch(ProtocolException pe) {
			// We have some sort of protocol exception. Get its status code and create response
			// We know only two kind of exception is possible inside fromInputStream
			// Protocol.BAD_REQUEST_CODE and Protocol.NOT_SUPPORTED_CODE
			int status = pe.getStatus();
			if(status == Protocol.BAD_REQUEST_CODE) {
				response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
			} else if(status == Protocol.NOT_SUPPORTED_CODE) {
				response = HttpResponseFactory.create505NotSupported(Protocol.CLOSE);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			// For any other error, we will create bad request response as well
			response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
		}
		
		if(response != null) {
			// there was an error, now write the response object to the socket
			try {
				response.write(outStream);
			}
			catch(Exception e){
				e.printStackTrace();
			}
			server.incrementConnections(1);
			// Get the end time
			long end = System.currentTimeMillis();
			this.server.incrementServiceTime(end-start);
			return;
		}
		
		try {
			if(!request.getVersion().equalsIgnoreCase(Protocol.VERSION)) {
				response = HttpResponseFactory.create505NotSupported(Protocol.CLOSE);
			} else if(request.getMethod().equalsIgnoreCase(Protocol.GET)||request.getMethod().equalsIgnoreCase(Protocol.HEAD)) {
				System.out.println("received a GET or HEAD request");
//				Map<String, String> header = request.getHeader();
//				String date = header.get("if-modified-since");
//				String hostName = header.get("host");
				
				String uri = request.getUri();
				String rootDirectory = server.getRootDirectory();
				File file = new File(rootDirectory + uri);
				if(file.exists()) {
					if(file.isDirectory()) {
						// Look for default index.html file in a directory
						String location = rootDirectory + uri + System.getProperty("file.separator") + Protocol.DEFAULT_FILE;
						file = new File(location);
						if(file.exists()) {
							response = HttpResponseFactory.create200OK(file, Protocol.CLOSE,request.getMethod().equalsIgnoreCase(Protocol.GET));
						}
						else {
							response = HttpResponseFactory.create404NotFound(Protocol.CLOSE);
						}
					}
					else { // Its a file
						response = HttpResponseFactory.create200OK(file, Protocol.CLOSE,request.getMethod().equalsIgnoreCase(Protocol.GET));
					}
				}
				else {
					response = HttpResponseFactory.create404NotFound(Protocol.CLOSE);
				}
			} else if(request.getMethod().equalsIgnoreCase(Protocol.POST)||request.getMethod().equalsIgnoreCase(Protocol.PUT)){
				System.out.println("received a POST or PUT request");
				File file = new File(server.getRootDirectory() + request.getUri());
				if(file.exists()&&!file.isDirectory()){
					// parse body for post/put data
					Map<String,String> values=getPutPostParameters(request.getBody(),request.getMethod().equalsIgnoreCase(Protocol.PUT));
					// TODO do something with this map once there is an application
					if(values.isEmpty())
						response = HttpResponseFactory.create200OK(file, Protocol.CLOSE,false);
					else
						response = HttpResponseFactory.create201Created(Protocol.CLOSE);
				} else
					response = HttpResponseFactory.create404NotFound(Protocol.CLOSE);				
			} else if(request.getMethod().equalsIgnoreCase(Protocol.DELETE)){
				System.out.println("received a DELETE request");
				File file = new File(server.getRootDirectory() + request.getUri());
				if(file.exists()&&!file.isDirectory()){
					System.out.println("DELETE request received for "+server.getRootDirectory() + request.getUri());
					// TODO actually delete the file (but not really)
					// file.delete();
					response = HttpResponseFactory.create200OK(file, Protocol.CLOSE,false);
				} else
					response = HttpResponseFactory.create404NotFound(Protocol.CLOSE);
			} else {
				// unknown method
				response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		try{
			response.write(outStream);
			socket.close();
		}
		catch(Exception e){
			e.printStackTrace();
		} 
		
		server.incrementConnections(1);
		long end = System.currentTimeMillis();
		this.server.incrementServiceTime(end-start);
	}


	/**
	 * @param body
	 * @return
	 */
	private Map<String, String> getPutPostParameters(String body,boolean put) {
		StringTokenizer tokenizer = new StringTokenizer(body, "&");
		HashMap<String,String> map = new HashMap<String,String>();
		while(tokenizer.hasMoreElements()){
			String pair=tokenizer.nextToken();
			int index= pair.indexOf('=');
			if(index==-1) continue;
			String key = pair.substring(0, index);
			String value = pair.substring(index+1);
			System.out.println(put?"PUT":"POST"+" request set: "+key +"="+ value);
			map.put(key,value);
		}
		return map;
	}
}
