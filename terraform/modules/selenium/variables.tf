variable "project_id" {
  description = "GCP project ID."
  type        = string
}

variable "region" {
  description = "GCP region for the Cloud Run service."
  type        = string
}

variable "environment" {
  description = "Environment label."
  type        = string
}

variable "service_name" {
  description = "Cloud Run service name. Must be unique per region/project."
  type        = string
}

variable "image" {
  description = "Selenium standalone-chrome container image."
  type        = string
  default     = "selenium/standalone-chrome:4.27.0"
}

variable "service_account_email" {
  description = "Runtime service account email used by the Cloud Run revision."
  type        = string
}

variable "vpc_connector_name" {
  description = "Serverless VPC Access connector name. Required so the browser-service can reach Selenium over the private VPC."
  type        = string
}

variable "port" {
  description = "Container port that Selenium listens on."
  type        = number
  default     = 4444
}

variable "memory_allocation" {
  description = "Memory limit for the Selenium container."
  type        = string
}

variable "cpu_allocation" {
  description = "CPU limit for the Selenium container."
  type        = string
}

variable "min_instances" {
  description = "Minimum warm Cloud Run instances. Selenium standalone benefits from min_instances=1 because cold starts include Chrome boot."
  type        = number
  default     = 1
}

variable "max_instances" {
  description = "Maximum Cloud Run instances. Selenium standalone serves a single session per container; keep this low (1-2)."
  type        = number
  default     = 1
}

variable "container_concurrency" {
  description = "Max concurrent requests per Selenium container. Selenium standalone-chrome handles 1 session at a time."
  type        = number
  default     = 1
}

variable "request_timeout_seconds" {
  description = "Cloud Run per-request timeout. Long-running Selenium operations need plenty of headroom."
  type        = number
  default     = 900
}

variable "ingress" {
  description = "Cloud Run ingress: 'all', 'internal', or 'internal-and-cloud-load-balancing'."
  type        = string
  default     = "internal"
}

variable "invoker_members" {
  description = "IAM members granted roles/run.invoker on this service (e.g. ['serviceAccount:browser-service@PROJECT.iam.gserviceaccount.com'])."
  type        = list(string)
  default     = []
}

variable "environment_variables" {
  description = "Extra environment variables to inject into the Selenium container."
  type        = map(string)
  default     = {}
}

variable "labels" {
  description = "Labels to apply to the Cloud Run service."
  type        = map(string)
  default     = {}
}
