#!/bin/bash
# =============================================================================
# gcp-iam-explorer.sh — Explore GCP IAM bindings, groups, and permissions
# Usage: ./gcp-iam-explorer.sh <command> [options]
# =============================================================================

set -euo pipefail

# ------------- Colors & Formatting -------------------------------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# ------------- Helpers -------------------------------------------------------
info()    { echo -e "${CYAN}[INFO]${NC} $*"; }
success() { echo -e "${GREEN}[OK]${NC}   $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }
header()  { echo -e "\n${BOLD}${BLUE}=== $* ===${NC}\n"; }

require() {
  command -v "$1" &>/dev/null || error "'$1' is not installed. Please install it first."
}

require_param() {
  [[ -z "${2:-}" ]] && error "Missing required parameter: $1. Run './gcp-iam-explorer.sh help' for usage."
}

# ------------- Usage ---------------------------------------------------------
usage() {
  cat <<EOF

${BOLD}gcp-iam-explorer.sh${NC} — GCP IAM & Group Explorer

${BOLD}USAGE:${NC}
  ./gcp-iam-explorer.sh <command> [options]

${BOLD}COMMANDS:${NC}

  ${CYAN}whoami${NC}
      Show your currently authenticated GCP identity and active project

  ${CYAN}my-projects${NC}
      List all GCP projects you have access to

  ${CYAN}my-roles${NC}  --project PROJECT_ID  [--email EMAIL]
      Show all roles assigned to your email (or given email) on a project

  ${CYAN}project-groups${NC}  --project PROJECT_ID
      List all Google Group bindings on a project (AD-synced groups + their roles)

  ${CYAN}project-iam${NC}  --project PROJECT_ID
      Full IAM policy dump for a project (all principals + roles)

  ${CYAN}folder-groups${NC}  --folder FOLDER_ID
      List all group bindings at a GCP folder level

  ${CYAN}org-groups${NC}  --org ORG_ID
      List all group bindings at the organization level (needs org viewer role)

  ${CYAN}all-my-bindings${NC}  --email EMAIL
      Scan all accessible projects and show where the email has bindings

  ${CYAN}troubleshoot${NC}  --project PROJECT_ID  --email EMAIL  --permission PERMISSION
      Check if an email has a specific permission and which binding grants it

  ${CYAN}effective-roles${NC}  --project PROJECT_ID  [--email EMAIL]
      Show effective roles for an email using Policy Analyzer

  ${CYAN}search-group${NC}  --group GROUP_EMAIL
      Show all IAM bindings for a specific group across accessible projects

  ${CYAN}compare-roles${NC}  --project PROJECT_ID  --email1 EMAIL1  --email2 EMAIL2
      Compare roles between two principals on the same project

  ${BOLD}EXAMPLES:${NC}
  ./gcp-iam-explorer.sh whoami
  ./gcp-iam-explorer.sh my-projects
  ./gcp-iam-explorer.sh my-roles --project cvs-prod-123
  ./gcp-iam-explorer.sh my-roles --project cvs-prod-123 --email sriram@cvshealth.com
  ./gcp-iam-explorer.sh project-groups --project cvs-prod-123
  ./gcp-iam-explorer.sh project-iam --project cvs-prod-123
  ./gcp-iam-explorer.sh folder-groups --folder 987654321
  ./gcp-iam-explorer.sh org-groups --org 112233445566
  ./gcp-iam-explorer.sh all-my-bindings --email sriram@cvshealth.com
  ./gcp-iam-explorer.sh troubleshoot --project cvs-prod-123 --email sriram@cvshealth.com --permission bigquery.tables.get
  ./gcp-iam-explorer.sh search-group --group gcp-devops-prod@cvshealth.com
  ./gcp-iam-explorer.sh compare-roles --project cvs-prod-123 --email1 sriram@cvshealth.com --email2 john@cvshealth.com

EOF
}

# ------------- Arg Parser ----------------------------------------------------
parse_args() {
  PROJECT=""
  EMAIL=""
  FOLDER=""
  ORG=""
  PERMISSION=""
  EMAIL1=""
  EMAIL2=""
  GROUP=""

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --project)    PROJECT="$2";    shift 2 ;;
      --email)      EMAIL="$2";      shift 2 ;;
      --folder)     FOLDER="$2";     shift 2 ;;
      --org)        ORG="$2";        shift 2 ;;
      --permission) PERMISSION="$2"; shift 2 ;;
      --email1)     EMAIL1="$2";     shift 2 ;;
      --email2)     EMAIL2="$2";     shift 2 ;;
      --group)      GROUP="$2";      shift 2 ;;
      *) warn "Unknown option: $1"; shift ;;
    esac
  done
}

# ------------- Commands ------------------------------------------------------

cmd_whoami() {
  header "Your GCP Identity"
  ACTIVE_ACCOUNT=$(gcloud auth list --filter=status:ACTIVE --format="value(account)" 2>/dev/null)
  ACTIVE_PROJECT=$(gcloud config get-value project 2>/dev/null)
  ORG_ID=$(gcloud organizations list --format="value(name)" 2>/dev/null | head -1 | sed 's/organizations\///' || echo "N/A")

  echo -e "  ${BOLD}Active Account :${NC} ${GREEN}${ACTIVE_ACCOUNT:-Not logged in}${NC}"
  echo -e "  ${BOLD}Active Project :${NC} ${ACTIVE_PROJECT:-Not set}"
  echo -e "  ${BOLD}Organization   :${NC} ${ORG_ID}"
  echo ""
  info "All authenticated accounts:"
  gcloud auth list 2>/dev/null || warn "Could not list auth accounts"
}

cmd_my_projects() {
  header "Projects You Have Access To"
  gcloud projects list \
    --format="table(projectId:label=PROJECT_ID, name:label=NAME, lifecycleState:label=STATE, createTime:label=CREATED)" \
    2>/dev/null || error "Could not list projects. Check your authentication."
}

cmd_my_roles() {
  require_param "--project" "$PROJECT"
  local email="${EMAIL:-$(gcloud auth list --filter=status:ACTIVE --format='value(account)' 2>/dev/null)}"

  header "Roles for $email on project: $PROJECT"
  info "Checking direct bindings for $email ..."

  RESULT=$(gcloud projects get-iam-policy "$PROJECT" \
    --flatten="bindings[].members" \
    --filter="bindings.members:${email}" \
    --format="table(bindings.role, bindings.members)" 2>/dev/null)

  if [[ -z "$RESULT" || "$RESULT" == "ROLE  MEMBERS" ]]; then
    warn "No direct binding found for $email"
    warn "Access may be coming via a group. Try: project-groups --project $PROJECT"
  else
    echo "$RESULT"
  fi
}

cmd_project_groups() {
  require_param "--project" "$PROJECT"
  header "Group Bindings on Project: $PROJECT"
  info "Showing all Google Groups (AD-synced) and their roles..."

  gcloud projects get-iam-policy "$PROJECT" \
    --flatten="bindings[].members" \
    --filter="bindings.members:group:" \
    --format="table(bindings.members:label=GROUP, bindings.role:label=ROLE)" \
    2>/dev/null | sort || error "Could not fetch IAM policy for project: $PROJECT"
}

cmd_project_iam() {
  require_param "--project" "$PROJECT"
  header "Full IAM Policy — Project: $PROJECT"
  info "All principals (users, groups, service accounts) and roles:"

  gcloud projects get-iam-policy "$PROJECT" \
    --flatten="bindings[].members" \
    --format="table(bindings.members:label=PRINCIPAL, bindings.role:label=ROLE)" \
    2>/dev/null | sort || error "Could not fetch IAM policy for project: $PROJECT"
}

cmd_folder_groups() {
  require_param "--folder" "$FOLDER"
  header "Group Bindings on Folder: $FOLDER"

  gcloud resource-manager folders get-iam-policy "$FOLDER" \
    --flatten="bindings[].members" \
    --filter="bindings.members:group:" \
    --format="table(bindings.members:label=GROUP, bindings.role:label=ROLE)" \
    2>/dev/null | sort || error "Could not fetch folder IAM. You may need folder viewer role."
}

cmd_org_groups() {
  require_param "--org" "$ORG"
  header "Group Bindings at Organization Level: $ORG"
  warn "Requires roles/resourcemanager.organizationViewer or roles/iam.organizationRoleViewer"

  gcloud organizations get-iam-policy "$ORG" \
    --flatten="bindings[].members" \
    --filter="bindings.members:group:" \
    --format="table(bindings.members:label=GROUP, bindings.role:label=ROLE)" \
    2>/dev/null | sort || error "Could not fetch org IAM. Check if you have org-level viewer role."
}

cmd_all_my_bindings() {
  require_param "--email" "$EMAIL"
  header "Scanning All Projects for Bindings: $EMAIL"
  warn "This may take a while if you have many projects..."

  PROJECTS=$(gcloud projects list --format="value(projectId)" 2>/dev/null)
  FOUND=0

  for proj in $PROJECTS; do
    RESULT=$(gcloud projects get-iam-policy "$proj" \
      --flatten="bindings[].members" \
      --filter="bindings.members:${EMAIL}" \
      --format="value(bindings.role)" 2>/dev/null)

    if [[ -n "$RESULT" ]]; then
      FOUND=$((FOUND + 1))
      echo -e "${GREEN}[FOUND]${NC} ${BOLD}$proj${NC}"
      echo "$RESULT" | while read -r role; do
        echo -e "        ${YELLOW}→${NC} $role"
      done
    fi
  done

  echo ""
  [[ $FOUND -eq 0 ]] && warn "No direct bindings found. Access is likely via group memberships." \
                      || success "Found direct bindings in $FOUND project(s)"
}

cmd_troubleshoot() {
  require_param "--project" "$PROJECT"
  require_param "--permission" "$PERMISSION"
  local email="${EMAIL:-$(gcloud auth list --filter=status:ACTIVE --format='value(account)' 2>/dev/null)}"

  header "Permission Check: $email → $PERMISSION on $PROJECT"
  info "Checking if $email has permission: $PERMISSION"
  echo ""

  # Check direct project bindings
  info "Step 1: Direct bindings on project..."
  gcloud projects get-iam-policy "$PROJECT" \
    --flatten="bindings[].members" \
    --filter="bindings.members:${email}" \
    --format="table(bindings.role, bindings.members)" 2>/dev/null

  # Check group bindings
  echo ""
  info "Step 2: Group bindings on project (your access likely comes from here)..."
  gcloud projects get-iam-policy "$PROJECT" \
    --flatten="bindings[].members" \
    --filter="bindings.members:group:" \
    --format="table(bindings.members:label=GROUP, bindings.role:label=ROLE)" 2>/dev/null

  echo ""
  info "Step 3: Use Policy Troubleshooter in console for exact trace:"
  echo -e "  ${CYAN}https://console.cloud.google.com/iam-admin/troubleshooter;email=${email};project=${PROJECT};permission=${PERMISSION}${NC}"
}

cmd_effective_roles() {
  require_param "--project" "$PROJECT"
  local email="${EMAIL:-$(gcloud auth list --filter=status:ACTIVE --format='value(account)' 2>/dev/null)}"

  header "Effective Roles: $email on $PROJECT"
  warn "Uses Cloud Asset API — requires roles/cloudasset.viewer"

  gcloud asset search-all-iam-policies \
    --scope="projects/${PROJECT}" \
    --query="policy.bindings.members:${email}" \
    --format="table(resource, policy.bindings.role, policy.bindings.members)" \
    2>/dev/null || warn "Cloud Asset API not available or insufficient permissions. Try 'my-roles' instead."
}

cmd_search_group() {
  require_param "--group" "$GROUP"
  header "Scanning All Projects for Group: $GROUP"
  warn "Scanning all accessible projects..."

  PROJECTS=$(gcloud projects list --format="value(projectId)" 2>/dev/null)
  FOUND=0

  for proj in $PROJECTS; do
    RESULT=$(gcloud projects get-iam-policy "$proj" \
      --flatten="bindings[].members" \
      --filter="bindings.members:${GROUP}" \
      --format="value(bindings.role)" 2>/dev/null)

    if [[ -n "$RESULT" ]]; then
      FOUND=$((FOUND + 1))
      echo -e "${GREEN}[FOUND]${NC} ${BOLD}$proj${NC}"
      echo "$RESULT" | while read -r role; do
        echo -e "        ${YELLOW}→${NC} $role"
      done
    fi
  done

  echo ""
  [[ $FOUND -eq 0 ]] && warn "Group $GROUP not found in any accessible project." \
                      || success "Group found in $FOUND project(s)"
}

cmd_compare_roles() {
  require_param "--project" "$PROJECT"
  require_param "--email1" "$EMAIL1"
  require_param "--email2" "$EMAIL2"

  header "Role Comparison on Project: $PROJECT"
  info "Email 1: $EMAIL1"
  info "Email 2: $EMAIL2"
  echo ""

  echo -e "${BOLD}--- $EMAIL1 ---${NC}"
  gcloud projects get-iam-policy "$PROJECT" \
    --flatten="bindings[].members" \
    --filter="bindings.members:${EMAIL1}" \
    --format="table(bindings.role)" 2>/dev/null || echo "  No direct bindings"

  echo ""
  echo -e "${BOLD}--- $EMAIL2 ---${NC}"
  gcloud projects get-iam-policy "$PROJECT" \
    --flatten="bindings[].members" \
    --filter="bindings.members:${EMAIL2}" \
    --format="table(bindings.role)" 2>/dev/null || echo "  No direct bindings"
}

# ------------- Main Dispatcher -----------------------------------------------
main() {
  require gcloud

  COMMAND="${1:-help}"
  shift || true
  parse_args "$@"

  case "$COMMAND" in
    whoami)            cmd_whoami ;;
    my-projects)       cmd_my_projects ;;
    my-roles)          cmd_my_roles ;;
    project-groups)    cmd_project_groups ;;
    project-iam)       cmd_project_iam ;;
    folder-groups)     cmd_folder_groups ;;
    org-groups)        cmd_org_groups ;;
    all-my-bindings)   cmd_all_my_bindings ;;
    troubleshoot)      cmd_troubleshoot ;;
    effective-roles)   cmd_effective_roles ;;
    search-group)      cmd_search_group ;;
    compare-roles)     cmd_compare_roles ;;
    help|--help|-h)    usage ;;
    *)
      error "Unknown command: '$COMMAND'. Run './gcp-iam-explorer.sh help' to see available commands."
      ;;
  esac
}

main "$@"
