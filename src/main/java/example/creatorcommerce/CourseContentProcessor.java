package example.creatorcommerce;

public final class CourseContentProcessor {
    public SubscriberUpdate prepareLesson(String lessonTitle, String assetPath) {
        return new SubscriberUpdate("New lesson: " + lessonTitle, "Your course has a new lesson at " + assetPath);
    }

    public record SubscriberUpdate(String subject, String body) {}
}
