# Legal Compliance Matrix

**Last verified:** 2026-08-17, against the official/primary sources cited per section.
This is an engineering-authored reference to inform the in-app legal drafts and to
give legal counsel a starting map of what to review — it is **not** a legal opinion,
and applicability calls marked "uncertain" or "review required" are exactly that:
unresolved, not decided in Orlune's favor by default.

Orlune's relevant technical facts, unchanged across every row below: no account, no
login, no backend server, no data transmitted off-device (no `INTERNET` permission),
all processing local to the device, no advertising or analytics SDK. Where a law's
obligations are principally about a party that *receives* or *transmits* personal
data, Orlune's local-only architecture reduces how much of that law's machinery is
triggered — but "reduces" is not "eliminates," and some of these frameworks reach
purely local processing too. Every "Orlune implementation" cell describes the
technical facts only; it does not assert a legal conclusion about applicability.

| Jurisdiction | Law/Regulation | Potential applicability | Relevant requirement | Orlune implementation | Document section | Open issue | Legal review required |
|---|---|---|---|---|---|---|---|
| India | Digital Personal Data Protection Act, 2023 + DPDP Rules, 2025 (notified 2025-11-13; phased enforcement through 2027-05-13) | Likely, if Orlune is offered to users in India (probable, given the product's origin) | Data Fiduciaries must have a lawful basis (consent or a listed "legitimate use"), limit processing to stated purpose, honor Data Principal rights (access, correction, erasure, grievance redressal), and notify breaches | All processing is on-device; the app publisher never receives a copy of any Data Principal's personal data. Export and delete are self-service, immediate, and require no request to Orlune. | Privacy Policy §17-20; Data Storage & Retention; Data Export & Deletion | Whether an app developer who never receives data but writes the code that processes it locally is a "Data Fiduciary" under the Act's broad definition of "processing" is not settled by this research and needs counsel's read | **Yes** — before any claim of DPDP compliance or non-applicability is published |
| India | DPDP Act, 2023 — children's/minor's data (Section 9) | Uncertain — depends on whether Orlune is found to process a child's personal data or is directed at children | Verifiable parental consent required to process a child's (under-18, under DPDP) personal data | No age-gating or account exists; no data is knowingly collected from anyone, child or adult, since nothing is transmitted off-device | Children & Teen Privacy | Whether on-device-only processing of a minor's own device-usage data triggers Section 9 at all | **Yes** |
| India | Information Technology Act, 2000 + IT (Reasonable Security Practices) Rules, 2011 | Likely, as a baseline for any Indian-offered software handling any personal information | "Reasonable security practices and procedures" for sensitive personal data | No network transmission surface exists (no INTERNET permission); standard Android app sandboxing; `allowBackup=false` | Security Statement | Whether device-usage patterns count as "sensitive personal data" under the Rules' enumerated categories | **Yes** |
| European Union | GDPR (Regulation (EU) 2016/679) | Uncertain — applies to processing of EU residents' personal data, including by non-EU controllers where they offer services to EU residents | Lawful basis (Art. 6), data-subject rights (access, erasure, portability), controller obligations, breach notification, cross-border transfer safeguards | On-device processing only; no controller-side data store exists for Orlune to search, export from, or breach in the traditional sense — self-service export/delete already gives the practical effect of access/erasure rights | Privacy Policy §17-20; Data Export & Deletion | Whether GDPR's broad definition of "processing" reaches purely local, non-transmitted processing performed by software the controller (the developer) authored, and whether the developer is a "controller" at all absent any data receipt | **Yes** |
| United Kingdom | UK GDPR + Data Protection Act 2018 (as amended by the Data (Use and Access) Act 2025, in force from 2026-02-05) | Uncertain, same reasoning as EU GDPR | Same core obligations as GDPR, now including "recognised legitimate interest" as a seventh lawful basis (from 2026-02-05) | Same as GDPR row | Privacy Policy §17-20 | Same as GDPR row | **Yes** |
| United States — California | CCPA/CPRA | Unlikely at current scale (no sale/sharing of data, likely below the "business" revenue/volume thresholds), but not verified | Opt-out-of-sale/sharing rights, disclosure of categories collected, minors' opt-in requirements (ages 13-16) | No data is sold or shared with any third party under any definition, so opt-out mechanics are moot; no data collection into a company-held store exists to disclose | Privacy Policy §6 (Data not collected) | Whether Orlune (the publisher) meets CCPA's "business" threshold definitions at any point post-launch | **Yes**, before any release into the California market at scale |
| United States — Federal | COPPA (as amended, full compliance date 2026-04-22) | Uncertain — depends on whether Orlune is found "directed to children under 13" or gains "actual knowledge" of a child user | Verifiable parental consent before collecting a child's personal information; new 2025-amendment retention/deletion and third-party-disclosure-consent requirements | No account or registration exists, so there is no age-collection point at all; no data leaves the device to be "collected" under COPPA's operative definitions | Children & Teen Privacy | Whether Orlune's screen-time/parental-interest subject matter alone risks an FTC "directed to children" finding regardless of actual demographics | **Yes** |
| Global (Google Play distribution) | Google Play Developer Program Policy — Data Safety, User Data, Permissions | Directly applicable the moment Orlune is submitted to Google Play | Accurate Data Safety declaration; privacy policy link in Play Console and in-app; permissions used only for their stated purpose | See `docs/google-play-privacy-compliance.md` for the full policy-by-policy mapping | All 15 documents (published-privacy-policy requirement); Data Collection & Permissions | Exact Data Safety form category selections have not yet been filled in Play Console — this matrix and the privacy policy are the inputs to that form, not a substitute for it | Recommended — Play Console's own review process is not a substitute for legal sign-off on the underlying claims |

## How to use this matrix

1. Before any public release, every "Legal review required" row must be resolved by
   qualified counsel, not inferred from this document.
2. If a jurisdiction's applicability is confirmed, the corresponding in-app document
   section must be updated from its current "not yet determined" / placeholder
   framing to a specific, accurate statement — and this matrix's "Open issue" column
   updated to reflect the resolution, not deleted.
3. This matrix should be re-verified whenever a cited law's enforcement phase
   advances (the DPDP Act's Phase 3 lands 2027-05-13; COPPA's 2025 amendments reach
   full compliance 2026-04-22) or when Orlune's actual data-handling changes.

## Sources

- [Digital Personal Data Protection Rules, 2025 Notified — India Briefing](https://www.india-briefing.com/news/dpdp-rules-2025-india-data-protection-law-compliance-40769.html/)
- [DPDPA 2023 Enforcement Timeline](https://www.dpdpa.com/dpdpa_enforcement_timeline.html)
- [India's Digital Personal Data Protection Regime Takes Effect — Lexology](https://www.lexology.com/library/detail.aspx?g=2073ac40-628f-4112-81f3-fffdfd4b8858)
- [UK Privacy Laws Explained: UK GDPR, DPA 2018, PECR & DUAA Guide (2026) — Didomi](https://www.didomi.io/blog/uk-privacy-laws-guide-gdpr-dpa-pecr-duaa)
- [Understanding the UK GDPR and How to Achieve Compliance — Usercentrics](https://usercentrics.com/knowledge-hub/uk-gdpr-compliance/)
- [Children's Online Privacy Protection Rule ("COPPA") — Federal Trade Commission](https://www.ftc.gov/legal-library/browse/rules/childrens-online-privacy-protection-rule-coppa)
- [COPPA Compliance 2026: The Amended FTC Rule, Now in Full Force — PrivacyLawMap](https://privacylawmap.com/blog/coppa-compliance-guide-2026)
- [App Store Privacy Laws: How State Accountability Acts Are Changing Mobile App Compliance in 2026 — PrivacyLawMap](https://privacylawmap.com/blog/app-store-privacy-laws-state-accountability-acts-2026)
