package pluginAPI;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.Protocol;

import java.util.ArrayList;

/**
 * Base request handler
 */
public abstract class ARequestHandler implements IRequestHandler {
    protected ArrayList<String> roots = new ArrayList<String>();

    @Override
    public boolean handlesPath(String path) {
        return roots.contains(path);
    }

    @Override
    public HttpResponse handleRequest(HttpRequest request) {
        switch (request.getMethod()) {
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
