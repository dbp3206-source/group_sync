# Cloud Infrastructure: Continuous Integration and Deployment

Continuous Integration and Continuous Deployment (CI/CD) automates the delivery of containerized applications.

## Automated Pipelines

- **Build Phase**: Compiles Java applications and produces immutable container images tagged with git commit SHAs.
- **Automated Testing**: Executes unit and integration test suites in isolated runner environments before container publication.
- **Deployment Strategy**: Supports rolling updates and zero-downtime blue/green switches on cloud hosting platforms.
