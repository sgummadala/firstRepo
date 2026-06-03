# GCP IAM Explorer Shell Script

> A single shell script dispatcher — pass different commands to run different GCP IAM investigation commands.
>
> **File location on your machine:** `~/gcp-iam-explorer.sh`

---

## Usage

```bash
chmod +x ~/gcp-iam-explorer.sh
~/gcp-iam-explorer.sh <command> [options]
```

---

## Available Commands

| Command | What It Does |
|---|---|
| `whoami` | Your active GCP identity, project, org |
| `my-projects` | All projects you can see |
| `my-roles` | Your roles on a specific project |
| `project-groups` | All AD groups + roles on a project ← most useful |
| `project-iam` | Full IAM dump — every principal |
| `folder-groups` | Group bindings at folder level |
| `org-groups` | Group bindings at org level (needs elevated access) |
| `all-my-bindings` | Scans every project for your email |
| `troubleshoot` | Debugs why you do/don't have a permission + links to console troubleshooter |
| `effective-roles` | Uses Cloud Asset API for deeper analysis |
| `search-group` | Finds a group across all projects |
| `compare-roles` | Compares two users' roles side-by-side |

---

## Examples

```bash
# Confirm you're logged in correctly
./gcp-iam-explorer.sh whoami

# See all projects
./gcp-iam-explorer.sh my-projects

# See AD groups and their roles on a project
./gcp-iam-explorer.sh project-groups --project cvs-prod-123456

# See your roles on a project
./gcp-iam-explorer.sh my-roles --project cvs-prod-123

# See your roles with explicit email
./gcp-iam-explorer.sh my-roles --project cvs-prod-123 --email sriram@cvshealth.com

# Full IAM dump on a project
./gcp-iam-explorer.sh project-iam --project cvs-prod-123

# Folder level groups
./gcp-iam-explorer.sh folder-groups --folder 987654321

# Org level groups (needs org viewer role)
./gcp-iam-explorer.sh org-groups --org 112233445566

# Scan all projects for your bindings
./gcp-iam-explorer.sh all-my-bindings --email sriram@cvshealth.com

# Why do I have/not have BigQuery access?
./gcp-iam-explorer.sh troubleshoot \
  --project cvs-prod-123 \
  --email sriram@cvshealth.com \
  --permission bigquery.tables.get

# Where is this group used?
./gcp-iam-explorer.sh search-group --group gcp-devops-prod@cvshealth.com

# Compare two users' access
./gcp-iam-explorer.sh compare-roles \
  --project cvs-prod-123 \
  --email1 sriram@cvshealth.com \
  --email2 john@cvshealth.com
```

---

## Script Structure

```
Lines 1–16    Setup        set -euo pipefail, color variables
Lines 18–31   Helpers      info, warn, error, header, require, require_param
Lines 33–95   usage()      Help text printed by the help command
Lines 97–121  parse_args() Parses all --flag value params into variables
Lines 123–333 Commands     One cmd_* function per command
Lines 335–363 main()       Checks gcloud exists, dispatches to the right cmd_*
```

---

## The Dispatcher Pattern

```bash
case "$COMMAND" in
  whoami)            cmd_whoami ;;
  my-projects)       cmd_my_projects ;;
  project-groups)    cmd_project_groups ;;
  ...
  *)  error "Unknown command" ;;
esac
```

**Adding a new command is just 3 steps:**
1. Write a `cmd_yourcommand()` function
2. Add one line to the `case` block
3. Add a description line to `usage()`

---

## Script Parameters Reference

| Parameter | Used By |
|---|---|
| `--project PROJECT_ID` | `my-roles`, `project-groups`, `project-iam`, `troubleshoot`, `effective-roles`, `compare-roles` |
| `--email EMAIL` | `my-roles`, `all-my-bindings`, `troubleshoot`, `effective-roles` |
| `--folder FOLDER_ID` | `folder-groups` |
| `--org ORG_ID` | `org-groups` |
| `--permission PERMISSION` | `troubleshoot` |
| `--group GROUP_EMAIL` | `search-group` |
| `--email1 / --email2` | `compare-roles` |

---

## Notes

- Script requires `gcloud` CLI to be installed and authenticated
- Run `gcloud auth login` first if not already authenticated
- For org/folder level commands you need elevated IAM roles — most engineers only have project-level access
- The `troubleshoot` command also prints a direct URL to the GCP Console Policy Troubleshooter
