# GCP IAM Bindings & What Principal Gets Passed from IdP

> **Question:** When I check IAM, I don't see `sriram@cvshealth.com` as a principal — what does get passed from the IdP?

---

## The Core Confusion: Your Email ≠ Your GCP Principal

When you log in via SSO, GCP does **not** create a principal called `sriram@cvshealth.com`. What gets created depends on **how** the identity federation is set up.

---

## Two Setup Patterns at Enterprise Scale

### Pattern 1: Google Workspace (most common at large corps)

CVS Health likely has a **Google Workspace** tenant (`cvshealth.com` domain). Your AD account gets **provisioned into Google Workspace** via SCIM sync.

```
Azure AD user: sriram@cvshealth.com
        │
        │ SCIM provisioning (GCDS or Azure AD SCIM connector)
        ▼
Google Workspace user: sriram@cvshealth.com
        │
        │ This IS a Google identity now
        ▼
GCP Principal: user:sriram@cvshealth.com
```

**In this case you WOULD see `user:sriram@cvshealth.com`** in IAM — but only if someone bound your individual account. More commonly you'd see the **group** binding, not your individual email.

> Check: in any GCP project → IAM → look for `group:gcp-somegroup@cvshealth.com` — your access comes through that, not your individual email.

---

### Pattern 2: Workforce Identity Federation (no Google Workspace)

If CVS does **not** provision everyone into Google Workspace, they use **Workforce Identity Federation** — your Azure AD identity maps to a **federated principal**, which looks completely different:

```
Azure AD user: sriram@cvshealth.com  (object ID: a1b2c3d4-...)
        │
        │ SAML assertion sent to GCP with attributes:
        │   - email: sriram@cvshealth.com
        │   - groups: [CVS-GCP-DevOps, CVS-GCP-DataEng]
        │   - objectId: a1b2c3d4-xxxx
        ▼
GCP Principal (NOT user:, but principal:// format):

principal://iam.googleapis.com/locations/global/workforcePools/cvs-workforce-pool/subject/sriram@cvshealth.com
```

**This is why you don't see your email directly** — the principal is a long `principal://` URI, not a simple `user:` entry.

---

## What You Actually See in IAM Bindings

```bash
# Individual user via Google Workspace
user:sriram@cvshealth.com

# Individual user via Workforce Identity Federation
principal://iam.googleapis.com/locations/global/workforcePools/POOL_ID/subject/SUBJECT

# Google Group (most common for corp access)
group:gcp-devops-prod@cvshealth.com

# All users in a Workforce Pool (broad access)
principalSet://iam.googleapis.com/locations/global/workforcePools/POOL_ID/*

# Users in pool matching an attribute (e.g. a specific AD group)
principalSet://iam.googleapis.com/locations/global/workforcePools/POOL_ID/attribute.department/engineering
```

---

## The SAML Token — What Actually Flows

When you hit SSO, Azure AD sends a **SAML assertion** (an XML blob) to Google. It contains:

```xml
<saml:Assertion>
  <saml:Subject>
    <saml:NameID>sriram@cvshealth.com</saml:NameID>   <!-- or objectGUID -->
  </saml:Subject>

  <saml:AttributeStatement>
    <saml:Attribute Name="http://schemas.microsoft.com/ws/2008/06/identity/claims/groups">
      <saml:AttributeValue>CVS-GCP-DevOps-Prod</saml:AttributeValue>
      <saml:AttributeValue>CVS-GCP-DataEng</saml:AttributeValue>
    </saml:Attribute>
    <saml:Attribute Name="email">
      <saml:AttributeValue>sriram@cvshealth.com</saml:AttributeValue>
    </saml:Attribute>
  </saml:AttributeStatement>
</saml:Assertion>
```

GCP's Workforce Identity Pool has **attribute mappings** that say:

```
google.subject  =  assertion.subject         →  becomes the principal "subject"
google.email    =  assertion.attributes.email
google.groups   =  assertion.attributes.groups   →  drives principalSet bindings
```

---

## How Your Access Actually Gets Resolved (end-to-end)

```
You log in
    │
    ▼
GCP receives SAML: subject=sriram@cvshealth.com, groups=[CVS-GCP-DevOps-Prod]
    │
    ▼
GCP checks IAM policy on project "cvs-prod":
    │
    ├─ Is there a binding for user:sriram@cvshealth.com ?          → No
    ├─ Is there a binding for principal://...subject/sriram... ?   → No
    ├─ Is there a binding for group:gcp-devops-prod@cvshealth.com? → YES ✓
    │         role: roles/editor
    ▼
Access granted — you see the project
```

**Your email never appears in IAM. The group binding is what carries your access.**

---

## Summary

| What you log in with | `sriram@cvshealth.com` via Azure AD SSO |
|---|---|
| **What GCP sees as principal** | Either `user:sriram@cvshealth.com` (if Google Workspace provisioned) or `principal://...workforcePools/.../subject/sriram@...` (if WIF) |
| **Where your access actually lives** | `group:gcp-devops-prod@cvshealth.com` → IAM role binding |
| **Why you don't see your email in IAM** | Access is granted to the **group**, not you individually — you inherit it via AD group membership |
| **What controls which projects you see** | Which AD groups you're in → which Google Groups you sync into → which IAM bindings those groups have |

The infra/platform team manages the group → IAM binding. Your AD team manages who's in the AD group. You sit at the intersection of both.
