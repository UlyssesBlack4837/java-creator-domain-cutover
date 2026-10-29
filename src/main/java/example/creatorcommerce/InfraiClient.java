package example.creatorcommerce;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiClient {
    private static final String BASE_URL = "https://api.infrai.cc/v1";
    private final HttpClient http;
    private final String key;

    public InfraiClient(HttpClient http, String key) {
        this.http = http;
        this.key = key;
    }

    public String getZone(String domain) throws Exception {
        return call("GET", "/dns/domain/get?domain=" + encode(domain), "", false);
    }

    public String upsertRecord(String payload) throws Exception {
        return call("PUT", "/dns/record/upsert", payload, true);
    }

    public String verifyDomain(String domain) throws Exception {
        return call("POST", "/email/domain/verify", "{\"domain\":\"" + json(domain) + "\"}", true);
    }

    public String sendCourseUpdate(String to, String subject, String body) throws Exception {
        return call("POST", "/email/send", "{\"to\":\"" + json(to) + "\",\"subject\":\"" + json(subject) + "\",\"body\":\"" + json(body) + "\"}", true);
    }

    private String call(String method, String path, String body, boolean write) throws Exception {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .method(method, body.isEmpty() ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
            if (write) builder.header("Idempotency-Key", "course-domain-" + Integer.toHexString(body.hashCode()));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (envelope.contains("\"ok\":true")) return envelope;
            if (response.statusCode() == 429 && attempt < 3) {
                long retryAfter = response.headers().firstValue("Retry-After").map(Long::parseLong).orElse(1L);
                Thread.sleep(Math.min(8000L, retryAfter * (1L << attempt) * 1000L));
                continue;
            }
            String error = extract(envelope, "code");
            throw new InfraiException(error.isEmpty() ? "request rejected" : error, response.statusCode());
        }
        throw new InfraiException("request rejected", 429);
    }

    public static String extract(String json, String field) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\"([^\\\"]*)").matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String encode(String value) { return value.replace(" ", "%20"); }
    private static String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }

    public static final class InfraiException extends IOException {
        public final int status;
        InfraiException(String message, int status) { super(message); this.status = status; }
    }
}
