package example.creatorcommerce;

public final class DomainCutoverServiceTest {
    public static void main(String[] args) {
        require(DomainCutoverService.cutoverAllowed(new DomainVerification("verified")), "verified permits cutover");
        require(!DomainCutoverService.cutoverAllowed(new DomainVerification("pending")), "pending retains incumbent");
        System.out.println("DomainCutoverServiceTest: PASS");
    }
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
