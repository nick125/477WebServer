import pluginAPI.ARequestHandler;
import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseType;

public class TestRequestHandler extends ARequestHandler {
    public TestRequestHandler()
    {
        roots.add("/TestPlugin");
        roots.add("/Tests/A/Nested/Path");
    }

    @Override
    public HttpResponse handleGET(HttpRequest request) {
        return HttpResponse.createResponse(HttpResponseType.OK, "Close",
                "Welcome to the test handler. You requested " + request.getUri() + " (relative: " + request.getRelativeUri() + ")!");
    }
}
