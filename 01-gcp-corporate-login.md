# GCP Corporate Login Process

> **Context:** How `sriram@cvshealth.com` logs into `console.cloud.google.com` and gets access to GCP projects via Active Directory groups.

---

## 1. Identity Federation — Your Corporate Login

When you log into `console.cloud.google.com` with a corporate email, GCP is **not** authenticating you directly. The flow is:

```
You → Google Login → CVS Health's Identity Provider (IdP) → Back to GCP
```

### What's actually happening:

- CVS Health uses an **IdP** — typically **Azure AD (Entra ID)**, Okta, or Ping — as the source of truth for identities
- Google Workspace (or **Workforce Identity Federation**) is configured to trust that IdP via **SAML 2.0** or **OIDC**
- When you type `sriram@cvshealth.com`, Google redirects you to CVS's login page (SSO), you authenticate there, and a signed assertion is sent back to Google saying "this is Sriram, here are his groups"

---

## 2. Active Directory Groups → GCP IAM Roles

This is the key mapping layer:

```
Azure AD Group  →  Google Group  →  GCP IAM Role Binding
─────────────────────────────────────────────────────────
CVS-GCP-DevOps-Prod    →  gcp-devops-prod@cvshealth.com   →  roles/editor on project cvs-prod
CVS-GCP-DataEng        →  gcp-dataeng@cvshealth.com        →  roles/bigquery.dataEditor
CVS-GCP-ReadOnly       →  gcp-readonly@cvshealth.com       →  roles/viewer on folder/org
```

### The wiring:
1. **AD Groups sync to Google Groups** — via **Google Cloud Directory Sync (GCDS)** or the IdP's provisioning (SCIM protocol)
2. **Google Groups are bound to IAM roles** — in GCP projects, folders, or org level
3. When you log in, your group memberships flow through and GCP evaluates what you can access

**Why you see different projects:** IAM bindings are set at the **project**, **folder**, or **org** level. Your AD group membership determines which resources you can see/touch.

---

## 3. GitHub Actions → GCP: Workload Identity Federation

GitHub Actions **does not use a service account key file** in modern setups (that's the old, insecure way). It uses **Workload Identity Federation (WIF)**:

```
GitHub Actions Job
      │
      │  OIDC token (JWT) issued by GitHub
      │  ("I am repo: cvs-health/my-repo, branch: main, workflow: deploy")
      ▼
GCP Workload Identity Pool
      │  Validates the JWT against GitHub's JWKS endpoint
      │  Checks attribute conditions (repo name, branch, etc.)
      ▼
Impersonates a GCP Service Account
      │
      ▼
Gets short-lived credentials (valid ~1 hour)
      │
      ▼
Deploys to GCP (Terraform, gcloud, Cloud Run, GKE, etc.)
```

### The GCP setup (done once by platform/infra team):

```hcl
# Workload Identity Pool
resource "google_iam_workload_identity_pool" "github" {
  workload_identity_pool_id = "github-actions-pool"
}

# Provider trusts GitHub's OIDC
resource "google_iam_workload_identity_pool_provider" "github" {
  provider_id = "github-provider"
  oidc { issuer_uri = "https://token.actions.githubusercontent.com" }

  # Only allow specific repos
  attribute_condition = "assertion.repository == 'cvs-health/infra-repo'"
  attribute_mapping = {
    "google.subject"       = "assertion.sub"
    "attribute.repository" = "assertion.repository"
    "attribute.ref"        = "assertion.ref"
  }
}

# Service account that the workflow impersonates
resource "google_service_account_iam_binding" "github_wif" {
  service_account_id = google_service_account.deploy_sa.name
  role               = "roles/iam.workloadIdentityUser"
  members = [
    "principalSet://iam.googleapis.com/.../attribute.repository/cvs-health/infra-repo"
  ]
}
```

### In the GitHub Actions workflow (`.github/workflows/deploy.yml`):

```yaml
jobs:
  deploy:
    permissions:
      id-token: write   # Required — allows GitHub to mint an OIDC token
      contents: read

    steps:
      - uses: google-github-actions/auth@v2
        with:
          workload_identity_provider: "projects/123/locations/global/workloadIdentityPools/github-actions-pool/providers/github-provider"
          service_account: "deploy-sa@cvs-prod.iam.gserviceaccount.com"

      - run: gcloud deploy ...   # Now authenticated as the service account
      - run: terraform apply     # Same
```

---

## 4. The Full Picture

```
┌─────────────────────────────────────────────────────────┐
│                    CVS Health Corp                       │
│                                                          │
│  Azure AD ──SCIM sync──► Google Workspace               │
│  (AD Groups)              (Google Groups)               │
│                                │                         │
│                                ▼                         │
│                    GCP Organization (cvshealth.com)      │
│                    ├── Folder: Production                │
│                    │   └── Project: cvs-prod             │
│                    │       IAM: gcp-devops → roles/editor│
│                    └── Folder: Data                      │
│                        └── Project: cvs-data             │
│                            IAM: gcp-dataeng → BQ roles   │
└─────────────────────────────────────────────────────────┘

GitHub Actions (external)
  │  OIDC token → WIF Pool → Service Account → deploys
  ▼
GCP Project (authorized by the infra team binding above)
```

---

## Key Concepts Summary

| Concept | What It Does |
|---|---|
| **SAML/OIDC SSO** | Federates your `@cvshealth.com` identity into Google |
| **GCDS / SCIM** | Keeps AD groups in sync with Google Groups |
| **IAM Bindings** | Maps Google Groups to GCP roles on projects/folders |
| **Workload Identity Federation** | Lets GitHub Actions authenticate without static keys |
| **Service Accounts** | GCP identity used by automated workloads (not humans) |
| **Org Policy / VPC-SC** | Additional guardrails your infra team adds at org/folder level |

The platform/infra team at CVS controls the IAM bindings — if you need access to a new project, they add your AD group to the right IAM binding on that project.
