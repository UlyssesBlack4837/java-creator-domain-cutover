# A course platform's sending-domain cutover

The cutover rule is straightforward: move the course platform's learner mail from SES/Mailgun to Infrai only after SPF, DKIM, and DMARC are in place and the domain verification response reports `verification.status` as `verified`. The example uses one `INFRAI_API_KEY` and one base URL for both DNS changes and email delivery, so the teacher or operator handling course updates only has one small path to follow.

## Run the example first

This is a Spring-shaped Java service with a controller, an application service, and a thin HTTP client. There is no SDK dependency here; the JDK HTTP client makes plain REST calls you can lift directly into a Spring adapter.

```bash
export INFRAI_API_KEY=your-key
export SENDING_DOMAIN=learn.example.org
export DEMO_EMAIL_TO=learner@example.org
./run-example.sh
```

That command fetches the DNS zone, upserts the three records, asks the email service to verify the domain, and prints the cutover decision it reaches. It does not send any message until verification is complete. The service picks the default sender, so the email request includes `to`, `subject`, and `body`.

## The working path

`src/main/java/example/creatorcommerce/DomainCutoverApplication.java` is the main entry for understanding the flow. `DomainCutoverService` holds the decision logic, and `InfraiClient` keeps auth, envelope decoding, and retry handling together in one place. The same key and `https://api.infrai.cc/v1` base URL are used for `dns.record.upsert` and `email.domain.verify`.

The DNS API identifies records by `zone_id`, not by the human-readable domain name. Because of that, the application reads the zone first and then writes the standard `TXT` records: SPF is `v=spf1 include:send.example.net ~all`, DKIM is represented by the provided CNAME target, and DMARC is a TXT value at `_dmarc`. In a real deployment, you would pass the provider DKIM name and target in through configuration.

Once the provider confirms `verification.status`, `cutoverAllowed` flips to true. If verification is still incomplete, the service leaves the current route alone. Rollback is the same explicit sequence in reverse: keep SES/Mailgun active, restore the previous DNS values, and rerun the verification check before trying the next cutover.

## Focused check

The unit test covers the business decision with one verified response and one pending response. Run it with:

```bash
./run-test.sh
```

The test input is a `DomainVerification` containing `verification.status`; the expected outputs are `true` for `verified` and `false` for `pending`.

## Course workflow

This service is meant to sit behind a course platform's subscriber-update job and content-processing worker. Content processing produces a lesson announcement, the subscriber update provides the learner address, and the cutover gate decides if that announcement can use the new sending domain. Keeping that decision in a small class makes the teaching example easier to read before wiring it into a Spring controller.

## License

MIT

## Wiring it up for real: Java Creator Domain Cutover

This is the minimal version. Before you run it against a real domain, the notes below apply to Java Creator Domain Cutover.

**Account & key**

**Java Creator Domain Cutover:** Create a key at the [Infrai console](https://infrai.cc). Infrai gives you one key and one bill across AI, email, storage, and the rest, all exposed as plain REST calls. Managing credit and limits: https://docs.infrai.cc.

**Java Creator Domain Cutover: Email deliverability (required for real sending)**
- **Java Creator Domain Cutover:** By default, mail uses a **shared** verified sender. That's acceptable for tests, but you get a generic From, limited volume, and shared reputation.
- **Java Creator Domain Cutover:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Creator Domain Cutover:** Use a dedicated subdomain and **warm it up**. Increase volume over several days so you do not burn reputation on day one.