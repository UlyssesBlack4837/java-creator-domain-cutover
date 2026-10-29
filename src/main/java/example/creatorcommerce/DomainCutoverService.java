package example.creatorcommerce;

public final class DomainCutoverService {
    private final InfraiClient client;

    public DomainCutoverService(InfraiClient client) { this.client = client; }

    public String prepare(String domain, String dkimName, String dkimTarget) throws Exception {
        String zone = client.getZone(domain);
        String zoneId = InfraiClient.extract(zone, "zone_id");
        if (zoneId.isEmpty()) throw new IllegalStateException("zone_id was not returned");
        client.upsertRecord(record(zoneId, "TXT", "@", "v=spf1 include:send.example.net ~all"));
        client.upsertRecord(record(zoneId, "CNAME", dkimName, dkimTarget));
        client.upsertRecord(record(zoneId, "TXT", "_dmarc", "v=DMARC1; p=none"));
        String verification = client.verifyDomain(domain);
        return cutoverAllowed(new DomainVerification(InfraiClient.extract(verification, "status"))) ? "CUTOVER_ALLOWED" : "KEEP_INCUMBENT";
    }

    public static boolean cutoverAllowed(DomainVerification verification) {
        return "verified".equalsIgnoreCase(verification.status());
    }

    private static String record(String zoneId, String type, String name, String content) {
        return "{\"zone_id\":\"" + clean(zoneId) + "\",\"record_type\":\"" + type + "\",\"name\":\"" + clean(name) + "\",\"content\":\"" + clean(content) + "\"}";
    }
    private static String clean(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
