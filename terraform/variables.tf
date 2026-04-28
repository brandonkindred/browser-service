#########################
# GCP project / auth
#########################

variable "project_id" {
  description = "The GCP project ID where infrastructure will be created."
  type        = string
}

variable "region" {
  description = "The GCP region used for regional resources (Cloud Run, VPC connector, Cloud SQL)."
  type        = string
  default     = "us-central1"
}

variable "environment" {
  description = "Deployment environment label (dev, staging, prod). Used as a suffix in Cloud Run, VPC connector, and service-account names — all of which have hard length limits, so this is capped at 12 chars."
  type        = string
  default     = "dev"

  validation {
    # 1 char OR 2-12 chars starting with a letter and ending with an
    # alphanumeric. The trailing-alnum requirement matters because composed
    # names like `bs-api-${env}` and `bs-conn-${env}` get fed to GCP APIs
    # that enforce RFC1035 (must end alphanumeric), so e.g. `dev-` would
    # plan-pass but apply-fail.
    condition     = can(regex("^[a-z]([a-z0-9-]{0,10}[a-z0-9])?$", var.environment))
    error_message = "environment must be 1-12 chars: lowercase letters/digits/hyphens, starting with a letter and ending with a letter or digit (no trailing hyphen). The cap keeps composed names under VPC connector (25), service account (30), and Cloud Run service (49) limits."
  }
}

variable "credentials_file" {
  description = "Optional path to a GCP service account credentials JSON file. Leave null to use Application Default Credentials (recommended)."
  type        = string
  default     = null
}

variable "labels" {
  description = "Additional labels to apply to all labeled resources."
  type        = map(string)
  default     = {}
}

#########################
# VPC
#########################

variable "vpc_name" {
  description = "Name of the VPC network to create. Defaults to 'browser-service-vpc-<environment>' so multiple environments in the same project don't collide."
  type        = string
  default     = null
}

variable "subnet_name" {
  description = "Name of the primary subnet. Defaults to 'browser-service-subnet-<environment>'."
  type        = string
  default     = null
}

variable "vpc_connector_name" {
  description = "Name of the Serverless VPC Access connector. Defaults to 'bs-conn-<environment>' (kept short to fit GCP's 25-char limit)."
  type        = string
  default     = null
}

variable "subnet_cidr" {
  description = "Primary subnet CIDR range used by the VPC."
  type        = string
  default     = "10.10.0.0/20"

  validation {
    condition     = can(cidrnetmask(var.subnet_cidr))
    error_message = "subnet_cidr must be a valid CIDR block (e.g. 10.10.0.0/20)."
  }
}

variable "connector_cidr" {
  description = "CIDR range for the Serverless VPC Access connector subnet (must be /28 — Serverless VPC Access requirement). Override if it overlaps with a custom `subnet_cidr`."
  type        = string
  default     = "10.8.0.0/28"

  validation {
    condition     = can(cidrnetmask(var.connector_cidr)) && endswith(var.connector_cidr, "/28")
    error_message = "connector_cidr must be a valid /28 CIDR block (e.g. 10.8.0.0/28)."
  }
}

#########################
# Browser Service (Spring Boot API)
#########################

variable "browser_service_image" {
  description = "Container image for the browser-service Spring Boot API."
  type        = string
  default     = "ghcr.io/brandonkindred/browser-service:latest"
}

variable "browser_service_service_name" {
  description = "Cloud Run service name for the browser-service API. Composed name is `${var}-${environment}` and must fit Cloud Run's 49-char limit; with environment capped at 12 chars, this base is capped at 36."
  type        = string
  default     = "browser-service-api"

  validation {
    condition     = can(regex("^[a-z]([a-z0-9-]{0,34}[a-z0-9])?$", var.browser_service_service_name))
    error_message = "browser_service_service_name must be 1-36 chars: lowercase letters/digits/hyphens, starting with a letter and ending with a letter or digit."
  }
}

variable "browser_service_min_instances" {
  description = "Minimum number of browser-service Cloud Run instances kept warm."
  type        = number
  default     = 1

  validation {
    condition     = var.browser_service_min_instances >= 0 && floor(var.browser_service_min_instances) == var.browser_service_min_instances
    error_message = "browser_service_min_instances must be a non-negative integer."
  }
}

variable "browser_service_max_instances" {
  description = "Maximum browser-service Cloud Run instance count."
  type        = number
  default     = 10

  validation {
    condition     = var.browser_service_max_instances >= 1 && floor(var.browser_service_max_instances) == var.browser_service_max_instances
    error_message = "browser_service_max_instances must be a positive integer."
  }

  validation {
    condition     = var.browser_service_max_instances >= var.browser_service_min_instances
    error_message = "browser_service_max_instances must be >= browser_service_min_instances."
  }
}

variable "browser_service_memory" {
  description = "Memory limit for the browser-service container (e.g. 1Gi, 2Gi)."
  type        = string
  default     = "2Gi"
}

variable "browser_service_cpu" {
  description = "CPU limit for the browser-service container (e.g. 1, 2)."
  type        = string
  default     = "2"
}

variable "browser_service_allow_public" {
  description = "If true, the browser-service API is invokable by allUsers. Set false to restrict to authenticated callers only — pair with `browser_service_invoker_members` to grant specific principals, otherwise the service has no IAM bindings and is unreachable."
  type        = bool
  default     = true
}

variable "browser_service_invoker_members" {
  description = "IAM principals granted roles/run.invoker on the browser-service API. Useful when `browser_service_allow_public = false` to grant specific service accounts or groups (e.g. ['serviceAccount:...', 'group:...']). Defaults to empty; combined with allow_public=false this means no IAM bindings and the API is unreachable until you set this."
  type        = list(string)
  default     = []
}

#########################
# Selenium (Cloud Run, fan-out)
#########################

variable "selenium_image" {
  description = "Selenium standalone Chrome image. Mirrors the LookseeIaC selenium module pattern."
  type        = string
  default     = "selenium/standalone-chrome:4.27.0"
}

variable "selenium_instance_count" {
  description = "Number of Selenium standalone-chrome Cloud Run instances to provision. Must be a positive integer — the API needs at least one Selenium endpoint to open sessions, and module count rejects fractional values."
  type        = number
  default     = 10

  validation {
    condition     = var.selenium_instance_count >= 1 && floor(var.selenium_instance_count) == var.selenium_instance_count
    error_message = "selenium_instance_count must be a positive integer (>= 1)."
  }
}

variable "selenium_min_instances" {
  description = "Per-replica minimum warm Cloud Run instances. Selenium standalone benefits from 1 because cold starts include Chrome boot; drop to 0 in dev to save cost."
  type        = number
  default     = 1

  validation {
    condition     = var.selenium_min_instances >= 0 && floor(var.selenium_min_instances) == var.selenium_min_instances
    error_message = "selenium_min_instances must be a non-negative integer."
  }
}

variable "selenium_max_instances" {
  description = "Per-replica max Cloud Run instance count. Selenium standalone serves a single session per container, so keep this low (1 by default)."
  type        = number
  default     = 1

  validation {
    condition     = var.selenium_max_instances >= 1 && floor(var.selenium_max_instances) == var.selenium_max_instances
    error_message = "selenium_max_instances must be a positive integer."
  }

  validation {
    condition     = var.selenium_max_instances >= var.selenium_min_instances
    error_message = "selenium_max_instances must be >= selenium_min_instances."
  }
}

variable "selenium_memory_allocation" {
  description = "Memory limit for each Selenium Cloud Run container."
  type        = string
  default     = "2Gi"
}

variable "selenium_cpu_allocation" {
  description = "CPU limit for each Selenium Cloud Run container."
  type        = string
  default     = "2"
}

variable "selenium_port" {
  description = "Container port that Selenium standalone-chrome listens on."
  type        = number
  default     = 4444

  validation {
    condition     = var.selenium_port >= 1 && var.selenium_port <= 65535 && floor(var.selenium_port) == var.selenium_port
    error_message = "selenium_port must be an integer in the range 1-65535."
  }
}

variable "selenium_invoker_members" {
  description = "IAM principals granted roles/run.invoker on each Selenium Cloud Run replica. Defaults to 'allUsers' so the Java client (which doesn't speak GCP id-token auth) can reach the services over the VPC; ingress=internal still keeps the public internet out. Set to [] or to specific service accounts in orgs that block allUsers via iam.allowedPolicyMemberDomains."
  type        = list(string)
  default     = ["allUsers"]
}

#########################
# Cloud SQL Postgres
#########################

variable "postgres_tier" {
  description = "Cloud SQL machine tier (e.g. db-custom-1-3840, db-f1-micro)."
  type        = string
  default     = "db-custom-1-3840"
}

variable "postgres_version" {
  description = "Cloud SQL Postgres engine version."
  type        = string
  default     = "POSTGRES_17"
}

variable "postgres_database_name" {
  description = "Name of the application database."
  type        = string
  default     = "browser_service"

  validation {
    condition     = can(regex("^[a-zA-Z][a-zA-Z0-9_]{0,62}$", var.postgres_database_name))
    error_message = "postgres_database_name must be 1-63 chars: letters/digits/underscore, starting with a letter."
  }
}

variable "postgres_user" {
  description = "Application database user."
  type        = string
  default     = "browser_service"

  validation {
    condition     = can(regex("^[a-zA-Z][a-zA-Z0-9_]{0,62}$", var.postgres_user))
    error_message = "postgres_user must be 1-63 chars: letters/digits/underscore, starting with a letter."
  }
}

variable "postgres_disk_size_gb" {
  description = "Disk size (GB) for the Cloud SQL instance. Cloud SQL minimum is 10GB."
  type        = number
  default     = 20

  validation {
    condition     = var.postgres_disk_size_gb >= 10 && floor(var.postgres_disk_size_gb) == var.postgres_disk_size_gb
    error_message = "postgres_disk_size_gb must be an integer >= 10 (Cloud SQL minimum)."
  }
}

variable "postgres_deletion_protection" {
  description = "Whether Cloud SQL deletion protection is enabled. Keep on for prod."
  type        = bool
  default     = true
}
