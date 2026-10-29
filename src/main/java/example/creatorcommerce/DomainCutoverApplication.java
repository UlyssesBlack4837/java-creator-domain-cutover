package example.creatorcommerce;

import java.net.http.HttpClient;

public final class DomainCutoverApplication {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        String domain = System.getenv("SENDING_DOMAIN");
        if (key == null || domain == null) throw new IllegalArgumentException("INFRAI_API_KEY and SENDING_DOMAIN are required");
        String recipient = System.getenv("DEMO_EMAIL_TO");
        if (recipient == null) throw new IllegalArgumentException("DEMO_EMAIL_TO is required");
        InfraiClient client = new InfraiClient(HttpClient.newHttpClient(), key);
        String result = new DomainCutoverService(client).prepare(domain, "selector1._domainkey", "selector1.dkim.example.net");
        if ("CUTOVER_ALLOWED".equals(result)) {
            CourseContentProcessor.SubscriberUpdate update = new CourseContentProcessor().prepareLesson("Email foundations", "/courses/email-foundations/lesson-4");
            String reply = client.sendCourseUpdate(recipient, update.subject(), update.body());
            System.out.println("domain=" + domain + " decision=" + result + " message_id=" + InfraiClient.extract(reply, "message_id"));
        } else {
            System.out.println("domain=" + domain + " decision=" + result + " route=incumbent");
        }
    }
}
