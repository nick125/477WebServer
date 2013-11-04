import pluginAPI.ARequestHandler;
import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseType;

import java.io.File;

public class FileRequestHandler extends ARequestHandler {
    public FileRequestHandler()
    {
        roots.add("/FileRequestPlugin");
    }

    @Override
    public HttpResponse handleGET(HttpRequest request) {
        if(validatePath(request.getRelativeUri())){
            return HttpResponse.createResponse(HttpResponseType.OK, "Close",
                    "Welcome to the FileRequestPlugin handler. You requested " + request.getUri() + " (relative: " + request.getRelativeUri() + ")!");
        }else{
            return HttpResponse.createResponse(HttpResponseType.Forbidden, "Close",
                    "You tried to access an invalid location. your ip has been reported " + request.getUri() + " (relative: " + request.getRelativeUri() + ")!");

        }

    }

    private boolean validatePath(String path){
        File f = new File("." + path);
        String absolutePath = f.getAbsolutePath();
        final String separator =  "file.separator";
        String p = System.getProperty(separator);
        String serverRoot =  p +"." + p;
        if(absolutePath.contains(serverRoot)){
            return true;
        }
        return false;
    }
}
