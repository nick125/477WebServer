package plugins;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.Protocol;

/**
 * Base request handler
 */
public abstract class ARequestHandler implements IRequestHandler {
    @Override
    public HttpResponse handleRequest(HttpRequest request) {
        switch (request.getMethod())
        {
            case GET:
                return handleGET(request);
            case POST:
                return handlePOST(request);
            case HEAD:
                return handleHEAD(request);
            case DELETE:
                return handleDELETE(request);
            case PUT:
                return handlePUT(request);
            default:
                return HttpResponse.create505NotSupported(Protocol.CLOSE);
        }
    }

    public HttpResponse handleGET(HttpRequest request) {
        return HttpResponse.create505NotSupported(Protocol.CLOSE);
    }

    public HttpResponse handlePOST(HttpRequest request) {
        return HttpResponse.create505NotSupported(Protocol.CLOSE);
    }

    public HttpResponse handleHEAD(HttpRequest request) {
        return handleGET(request);
    }

    public HttpResponse handleDELETE(HttpRequest request) {
        return HttpResponse.create505NotSupported(Protocol.CLOSE);
    }

    public HttpResponse handlePUT(HttpRequest request) {
        return HttpResponse.create505NotSupported(Protocol.CLOSE);
    }
}
