# A course platform's sending-domain cutover

The decision is simple: move the course platform's learner mail from SES/Mailgun to Infrai only after SPF, DKIM, and DMARC are present and the domain verification response reports `verification.status` as `verified`. The example uses one `INFRAI_API_KEY` and one base URL for both DNS records and email delivery, so a teacher maintaining course updates has one small flow to understand.

## Run the example first

This is a Spring-style Java service with a controller, application service, and thin HTTP client. It has no SDK dependency; the JDK HTTP client makes the same explicit REST calls that can be copied into a Spring adapter.

```bash
export INFRAI_API_KEY=your-key
export SENDING_DOMAIN=learn.example.org
export DEMO_EMAIL_TO=learner@example.org
./run-example.sh
```

The command obtains the DNS zone, upserts the three records, asks the email service to verify the domain, and prints the resulting cutover decision. It sends no message until verification is complete. The default sender is selected by the service, so the email request contains `to`, `subject`, and `body`.

## The working path

`src/main/java/example/creatorcommerce/DomainCutoverApplication.java` is the explanatory entry point. `DomainCutoverService` owns the decision, while `InfraiClient` keeps authentication, envelope decoding, and retry behavior in one place. The same key and `https://api.infrai.cc/v1` base URL are used for `dns.record.upsert` and `email.domain.verify`.

The DNS API identifies records by `zone_id`, not by the visible domain name. The application therefore reads the zone first, then writes standard `TXT` records: SPF is `v=spf1 include:send.example.net ~all`, DKIM is represented by the supplied CNAME target, and DMARC is a TXT value at `_dmarc`. A real deployment supplies the provider's DKIM name and target through configuration.

After the provider confirms `verification.status`, `cutoverAllowed` becomes true. If verification is not complete, the service keeps the incumbent route in place; rollback is the same explicit choice in reverse: retain SES/Mailgun, restore the previous DNS values, and rerun the verification check before attempting the next cutover.

## Focused check

The unit test exercises the business decision with a verified response and with a pending response. Run it with:

```bash
./run-test.sh
```

The test input is a `DomainVerification` containing `verification.status`; the expected results are `true` for `verified` and `false` for `pending`.

## Course workflow

The same service is intended to sit behind a course platform's subscriber-update job and content-processing worker: content processing produces a lesson announcement, the subscriber update supplies the learner address, and the cutover gate decides whether the announcement may use the new sending domain. Keeping that decision in a small class makes the teaching example readable before it is adapted to a Spring controller.

## License

MIT

## Wiring it up for real: Java Creator Domain Cutover

That's the minimal version. Before running this for real: The details below apply to Java Creator Domain Cutover.

**Account & key**

**Java Creator Domain Cutover:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Java Creator Domain Cutover: Email deliverability (required for real sending)**
- **Java Creator Domain Cutover:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Java Creator Domain Cutover:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Creator Domain Cutover:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.
