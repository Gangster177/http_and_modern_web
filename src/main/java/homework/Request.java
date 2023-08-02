package homework;

import org.apache.http.NameValuePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {
    private final String method;
    private final String path;
    private Map<String, List<String>> params;

    public Request (String method, String path, List<NameValuePair> params){
        this.method = method;
        this.path = path;
        addParams(params);
    }

    private void addParams(List<NameValuePair> params) {
        for (NameValuePair nvp : params){
            List<String> list = this.params.computeIfAbsent(nvp.getName(),k-> new ArrayList<>());
            list.add(nvp.getValue());
        }
    }

    public String getMethod(){
        return method;
    }

    public String getPath(){
        return path;
    }

    public List<String> getQueryParam(String name) {
        return new ArrayList<>(params.getOrDefault(name, new ArrayList<>()));
    }

    public Map<String, List<String>> getQueryParams() {
        return new HashMap<>(params);
    }
}
