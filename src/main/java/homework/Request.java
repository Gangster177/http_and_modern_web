package homework;

import org.apache.http.NameValuePair;

import java.util.*;

public class Request {
    private final String method;
    private final String path;//только путь без запросов
    private final Map<String, List<String>> paramsQueryList = new HashMap<>();
    private final List<NameValuePair> paramsQuery;
    private final List<NameValuePair> paramsBody;

    public Request(String method, String path, List<NameValuePair> paramsQuery, List<NameValuePair> paramsBody) {
        this.method = method;
        this.path = path;
        this.paramsQuery = paramsQuery;
        this.paramsBody = paramsBody;
        setQueryParams();
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    private void setQueryParams() {
        var query = method.equals("GET") ? paramsQuery : paramsBody;
        for (NameValuePair nvp : query) {
            if (!paramsQueryList.containsKey(nvp.getName())) {
                paramsQueryList.put(nvp.getName(), new ArrayList<>());
            }
            paramsQueryList.get(nvp.getName()).add(nvp.getValue());
        }

    }

    public Map<String, List<String>> getQueryParams() {
        return paramsQueryList;
    }

    public String printQueryParams() {
        return Arrays.deepToString(getQueryParams().entrySet().toArray());
    }

    public String getQueryParam(String name) {
        return getQueryParams().get(name).toString();
    }
}
