variable "project_id" {
  description = "GCP project ID."
  type        = string
}

variable "region" {
  description = "GCP region for the Cloud SQL instance."
  type        = string
}

variable "environment" {
  description = "Environment label, used in resource naming."
  type        = string
}

variable "instance_name" {
  description = "Base name for the Cloud SQL instance (environment is appended)."
  type        = string
  default     = "browser-service-pg"
}

variable "database_version" {
  description = "Cloud SQL Postgres engine version (e.g. POSTGRES_17)."
  type        = string
  default     = "POSTGRES_17"
}

variable "tier" {
  description = "Cloud SQL machine tier."
  type        = string
}

variable "disk_size_gb" {
  description = "Initial disk size in GB."
  type        = number
  default     = 20
}

variable "availability_type" {
  description = "ZONAL for single-zone (cheaper) or REGIONAL for HA."
  type        = string
  default     = "ZONAL"
}

variable "network_id" {
  description = "VPC network self-link / id used for private IP."
  type        = string
}

variable "private_vpc_connection" {
  description = "Service networking connection id (depends_on hook so private IP is reachable before the SQL instance is created)."
  type        = string
}

variable "database_name" {
  description = "Application database name."
  type        = string
}

variable "database_user" {
  description = "Application database user."
  type        = string
}

variable "service_account_email" {
  description = "Email of the runtime service account that needs read access to the generated password secret."
  type        = string
}

variable "deletion_protection" {
  description = "Whether Cloud SQL deletion protection is enabled."
  type        = bool
  default     = true
}

variable "labels" {
  description = "Labels to apply to the Cloud SQL instance and secrets."
  type        = map(string)
  default     = {}
}
