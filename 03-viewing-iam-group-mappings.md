# Viewing AD Group → GCP Role Mappings

> **Question:** Can I see what AD groups and GCP roles are mapped, and what roles have been assigned?

---

## What You Can See (Depends on Your Access Level)

Most regular users can see bindings on projects they have access to, but **org-level visibility requires elevated roles**
(`roles/iam.organizationRoleViewer` or `roles/resourcemanager.organizationViewer`).

---

## Option 1: GCP Console (Easiest)

**Project level:**
```
console.cloud.google.com → Select Project → IAM & Admin → IAM
```
- Check **"Include Google-provided role grants"** checkbox
- You'll see all `group:`, `user:`, `serviceAccount:` principals and their roles
- Filter by `cvshealth.com` to find your corp groups

**Org/Folder level** (if you have access):
```
IAM & Admin → IAM → change scope to Organization or Folder at the top
```

---

## Option 2: gcloud CLI Commands

### See all bindings on a specific project:
```bash
gcloud projects get-iam-policy PROJECT_ID \
  --flatten="bindings[].members" \
  --format="table(bindings.role, bindings.members)" \
  | grep -i "cvshealth\|group:"
```

### Filter only group bindings (your AD-synced groups):
```bash
gcloud projects get-iam-policy PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:group:" \
  --format="table(bindings.members:label=GROUP, bindings.role:label=ROLE)"
```

**Output looks like:**
```
MEMBERS                                    ROLE
group:gcp-devops-prod@cvshealth.com        roles/editor
group:gcp-dataeng@cvshealth.com            roles/bigquery.dataEditor
group:gcp-readonly@cvshealth.com           roles/viewer
```

### See bindings at folder level:
```bash
gcloud resource-manager folders get-iam-policy FOLDER_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:group:" \
  --format="table(bindings.members:label=GROUP, bindings.role:label=ROLE)"
```

### See bindings at org level:
```bash
gcloud organizations get-iam-policy ORG_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:group:" \
  --format="table(bindings.members:label=GROUP, bindings.role:label=ROLE)"
```

---

## Option 3: Find YOUR Effective Permissions Specifically

### What roles do YOU have on a project (via any group):
```bash
gcloud projects get-iam-policy PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:sriram@cvshealth.com" \
  --format="table(bindings.role, bindings.members)"
```

### The better way — Policy Analyzer (requires Security Command Center or org access):
```bash
gcloud asset search-all-iam-policies \
  --scope="projects/PROJECT_ID" \
  --query="policy.bindings.members:sriram@cvshealth.com" \
  --format="table(resource, policy.bindings.role, policy.bindings.members)"
```

---

## Option 4: Policy Troubleshooter (Console — Best for Debugging)

```
console.cloud.google.com → IAM & Admin → Policy Troubleshooter
```
- Enter your email: `sriram@cvshealth.com`
- Enter a resource (project, bucket, dataset)
- Enter a permission (e.g. `bigquery.tables.get`)
- It tells you **exactly which group binding granted you that permission**

This is the most useful tool — it traces the exact path from your identity → group → binding → role → permission.

---

## Option 5: List All Projects You Have Access To

```bash
# See all projects your identity can see
gcloud projects list --format="table(projectId, name, lifecycleState)"

# For each project, see why you have access
for project in $(gcloud projects list --format="value(projectId)"); do
  echo "=== $project ==="
  gcloud projects get-iam-policy $project \
    --flatten="bindings[].members" \
    --filter="bindings.members:sriram@cvshealth.com OR bindings.members:group:*cvshealth*" \
    --format="table(bindings.members, bindings.role)" 2>/dev/null
done
```

---

## Realistic Limitations at CVS Health

| What you want | Likely possible? |
|---|---|
| IAM bindings on projects you have access to | **Yes** — viewer role is enough |
| IAM bindings at folder level | **Maybe** — need folder viewer |
| IAM bindings at org level | **Probably not** — restricted to infra/security team |
| Which AD groups map to which Google Groups | **Ask your IT/platform team** — GCDS config is internal |
| Full org-wide group→role report | **Platform team** — they use Security Command Center or Cloud Asset Inventory |

---

## The Authoritative Source: Cloud Asset Inventory

If you have access, this pulls **all IAM policies across the org**:

```bash
gcloud asset search-all-iam-policies \
  --scope="organizations/ORG_ID" \
  --query="policy.bindings.members:group:*cvshealth.com" \
  --format=json | jq '.[] | {resource: .resource, role: .policy.bindings[].role, members: .policy.bindings[].members}'
```

> Most likely your platform team has a **shared spreadsheet or Confluence page** that documents the group → role mappings — that's the practical answer for a corp environment where most engineers don't have org-level IAM visibility.
