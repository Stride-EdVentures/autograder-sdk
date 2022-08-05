package sdk;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class RestQueryBuilder {
    private final String url;
    private final Map<String, String> queryParameters;

    public RestQueryBuilder(String tableName) {
        this.url = "/rest/v1/" + tableName;
        this.queryParameters = new HashMap<>();
    }

    public static RestQueryBuilder from(String tableName) {
        return new RestQueryBuilder(tableName);
    }

    public RestQueryBuilder select(String columns) {
        this.queryParameters.merge("select", columns, (previousColumns, currentColumns) -> previousColumns + "," + currentColumns);
        return this;
    }

    public RestQueryBuilder equals(String column, Object value) {
        this.queryParameters.put(column, "eq." + value.toString());
        return this;
    }

    public RestQueryBuilder notEquals(String column, Object value) {
        this.queryParameters.put(column, "neq." + value.toString());
        return this;
    }

    public String generateQuery() throws UnsupportedEncodingException {
        StringBuilder queryString = new StringBuilder();

        for (Map.Entry<String, String> entry : this.queryParameters.entrySet()) {
            queryString.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            queryString.append("=");
            queryString.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            queryString.append("&");
        }

        if (queryString.length() > 0) {
            queryString.delete(queryString.length() - 1, queryString.length());
        }

        return String.format("%s?%s", this.url, queryString);
    }
}
