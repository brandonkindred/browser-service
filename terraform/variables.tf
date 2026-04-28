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
    condition     = can(regex("^[a-z][a-z0-9-]{0,11}$", var.environment))
    error_message = "environment must be 1-12 chars, lowercase letters/digits/hyphens, starting with a letter. The cap keeps composed names under VPC connector (25), service account (30), and Cloud Run service (49) limits."
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
  description = "Name of the Serverless VPC Access connector. Defaults to 'browser-service-conn-<environment>'."
  type        = string
  default     = null
}

variable "subnet_cidr" {
  description = "Primary subnet CIDR range used by the VPC."
  type        = string
  default     = "10.10.0.0/20"
}

variable "vpc_enable_nat" {
  description = "Provision a Cloud Router + Cloud NAT so traffic exiting the VPC connector (egress=all-traffic) can reach public non-Google endpoints. Set false in environments that don't need external egress."
  type        = bool
  default     = true
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
  description = "Cloud Run service name for the browser-service API."
  type        = string
  default     = "browser-service-api"
}

variable "browser_service_min_instances" {
  description = "Minimum number of browser-service Cloud Run instances kept warm."
  type        = number
  default     = 1
}

variable "browser_service_max_instances" {
  description = "Maximum browser-service Cloud Run instance count."
  type        = number
  default     = 10

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
  description = "If true, the browser-service API is invokable by allUsers. Set false to restrict to authenticated callers only."
  type        = bool
  default     = true
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
  description = "Number of Selenium standalone-chrome Cloud Run instances to provision. Must be at least 1 — the API needs at least one Selenium endpoint to open sessions."
  type        = number
  default     = 10

  validation {
    condition     = var.selenium_instance_count >= 1
    error_message = "selenium_instance_count must be at least 1 (the API needs at least one Selenium endpoint)."
  }
}

variable "selenium_min_instances" {
  description = "Per-replica minimum warm Cloud Run instances. Selenium standalone benefits from 1 because cold starts include Chrome boot; drop to 0 in dev to save cost."
  type        = number
  default     = 1
}

variable "selenium_max_instances" {
  description = "Per-replica max Cloud Run instance count. Selenium standalone serves a single session per container, so keep this low (1 by default)."
  type        = number
  default     = 1

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
}

variable "postgres_user" {
  description = "Application database user."
  type        = string
  default     = "browser_service"
}

variable "postgres_disk_size_gb" {
  description = "Disk size (GB) for the Cloud SQL instance."
  type        = number
  default     = 20
}

variable "postgres_deletion_protection" {
  description = "Whether Cloud SQL deletion protection is enabled. Keep on for prod."
  type        = bool
  default     = true
}
