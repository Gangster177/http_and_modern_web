package homework;
// TODO тело запроса, если есть.
public class Request {
    private final String method;
    private final String path;

    public Request (String[] parts){
        this.method = parts[0];
        this.path = parts[1];

    }

    public String getMethod(){
        return method;
    }

    public String getPath(){
        return path;
    }
}
