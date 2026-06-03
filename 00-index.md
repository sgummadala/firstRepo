# GCP Corporate Identity & IAM — Session Notes

> Notes from a learning session on GCP corporate login, IAM bindings, and the IAM explorer script.
> **Date:** 2026-06-02

---

## Files in This Folder

| File | Topic |
|---|---|
| [01-gcp-corporate-login.md](./01-gcp-corporate-login.md) | How corporate SSO login works, AD groups → GCP IAM, GitHub Actions WIF |
| [02-iam-bindings-and-principals.md](./02-iam-bindings-and-principals.md) | What principal gets passed from IdP, SAML token flow, why your email isn't in IAM |
| [03-viewing-iam-group-mappings.md](./03-viewing-iam-group-mappings.md) | How to see AD group → GCP role mappings via console, gcloud CLI, Policy Troubleshooter |
| [04-gcp-iam-explorer-script.md](./04-gcp-iam-explorer-script.md) | Shell script reference — commands, examples, structure |

---

## Quick Reference

### Login Flow
```
You (sriram@cvshealth.com)
  → Azure AD SSO
  → SAML assertion sent to Google
  → Google maps groups to IAM bindings
  → You see GCP projects your AD groups have access to
```

### Why You Don't See Your Email in IAM
- Access comes via **Google Groups** (synced from AD groups)
- IAM binding is on the **group**, not your individual email
- Your email → AD group → Google group → IAM role → project access

### Key gcloud Commands
```bash
# Who am I?
gcloud auth list

# What projects can I see?
gcloud projects list

# What groups have access to a project?
gcloud projects get-iam-policy PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:group:" \
  --format="table(bindings.members, bindings.role)"
```

### IAM Explorer Script
```bash
# Script is saved at:
~/gcp-iam-explorer.sh

# Make executable and run:
chmod +x ~/gcp-iam-explorer.sh
~/gcp-iam-explorer.sh help
~/gcp-iam-explorer.sh whoami
~/gcp-iam-explorer.sh project-groups --project YOUR_PROJECT_ID
```

---

## Key Concepts

| Term | Meaning |
|---|---|
| **SAML / OIDC** | Protocols used for SSO — Azure AD sends a signed token to Google |
| **GCDS / SCIM** | Syncs AD groups into Google Groups automatically |
| **IAM Binding** | Attaches a principal (group/user) to a role on a resource |
| **Workload Identity Federation (WIF)** | Lets GitHub Actions authenticate to GCP without static keys |
| **Service Account** | GCP identity for automated workloads (not humans) |
| **principalSet://** | The URI format for federated identities (Workforce Identity) |
| **Policy Troubleshooter** | GCP console tool that traces exactly why you have/don't have a permission |
