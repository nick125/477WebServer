package protocol;

/**
 * Various HTTP Response types
 */
public enum HttpResponseType {
    // 1xx
    SwitchingProtocols(101, "Switching Protocols"),
    // 2xx
    OK(200, "OK"),
    Created(201, "Created"),
    Accepted(202, "Accepted"),
    NoContent(204, "No Content"),
    // 3xx
    MovedPermanently(301, "MovedPermanently"),
    Found(302, "Found"),
    NotModified(304, "Not Modified"),
    // 4xx
    BadRequest(400, "Bad Request"),
    Unauthorized(401, "Unauthorized"),
    Forbidden(403, "Forbidden"),
    NotFound(404, "Not Found"),
    Gone(410, "Gone"),
    ImATeapot(418, "I'm a teapot"),
    // 5xx
    InternalServerError(500, "Internal Server Error"),
    NotImplemented(501, "Not Implemented"),
    ServiceNotAvailable(503, "Service Unavailable"),
    NotSupported(505, "HTTP Version Not Supported");

    private final int code;
    private final String text;
    HttpResponseType(int code, String text) {
        this.code = code;
        this.text = text;
    }

    int getCode()
    {
        return this.code;
    }

    String getText()
    {
        return this.text;
    }

    String getFullType()
    {
        return String.format("%d %s", this.code, this.text);
    }
}
