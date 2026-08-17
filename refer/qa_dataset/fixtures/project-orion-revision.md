# Project Orion Revision Notes

Addendum to the Project Orion deployment guideline.

## Revision Details

- **Admin Port Override**: In staging environments, the administrative daemon can alternatively listen on port `9443` if port `8443` is occupied.
- **Protocol Amendment**: The `RFC-9421` requirement is enforced on all ingress endpoints starting from Version 2.4.
- **Audit Logging**: All cryptographic operations must emit structured JSON logs to stdout.
