package plugins;

import protocol.HttpRequest;
import protocol.HttpResponse;

/**
 * A basic request handler
 */
public interface IRequestHandler {
    HttpResponse handleRequest(HttpRequest request);
}
