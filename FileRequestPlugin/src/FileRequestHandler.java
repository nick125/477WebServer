import pluginAPI.ARequestHandler;
import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseType;
import protocol.Protocol;

import java.io.File;

public class FileRequestHandler extends ARequestHandler {
    public FileRequestHandler()
    {
        roots.add("/FileRequestPlugin");
    }

    @Override
    public HttpResponse handleGET(HttpRequest request) {
        if(validatePath(request.getRelativeUri())){
            if(request.getHeaders().containsKey("if-none-match")){
                String oldTag = request.getHeader("if-none-match");
                File f = new File(request.getRelativeUri().substring(1));
                String lastModified =  f.lastModified()+"";
                if(lastModified.equals(oldTag)){
                   return  HttpResponse.create304NotModified(
                           "Welcome to the FileRequestPlugin handler. You requested " + request.getUri() + " (relative: " + request.getRelativeUri() + ")!");
                }
            }
            HttpResponse response =  HttpResponse.createResponse(HttpResponseType.OK, "Close",
                    "Welcome to the FileRequestPlugin handler. You requested " + request.getUri() + " (relative: " + request.getRelativeUri() + ")!");
            File f = new File(request.getRelativeUri().substring(1));
            response.putETag(f.lastModified()+"");
            return response;
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
