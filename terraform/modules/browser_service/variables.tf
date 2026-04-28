variable "project_id" {
  description = "GCP project ID."
  type        = string
}

variable "region" {
  description = "GCP region for the Cloud Run service."
  type        = string
}

variable "service_name" {
  description = "Cloud Run service name."
  type        = string
}

variable "image" {
  description = "Container image for the browser-service Spring Boot API."
  type        = string
}

variable "port" {
  description = "Container port the Spring Boot app listens on (matches application.yaml server.port)."
  type        = number
  default     = 8080
}

variable "service_account_email" {
  description = "Runtime service account email."
  type        = string
}

variable "vpc_connector_name" {
  description = "Serverless VPC Access connector name (so Cloud Run can reach Cloud SQL private IP and internal Selenium services)."
  type        = string
}

variable "min_instances" {
  description = "Minimum warm Cloud Run instances."
  type        = number
  default     = 1
}

variable "max_instances" {
  description = "Maximum Cloud Run instances."
  type        = number
  default     = 10
}

variable "container_concurrency" {
  description = "Max concurrent requests per container."
  type        = number
  default     = 80
}

variable "request_timeout_seconds" {
  description = "Cloud Run per-request timeout. Browser sessions may take a while to set up."
  type        = number
  default     = 600
}

variable "memory" {
  description = "Memory limit for the container (e.g. 1Gi, 2Gi)."
  type        = string
  default     = "2Gi"
}

variable "cpu" {
  description = "CPU limit for the container."
  type        = string
  default     = "2"
}

variable "ingress" {
  description = "Cloud Run ingress: 'all', 'internal', or 'internal-and-cloud-load-balancing'."
  type        = string
  default     = "all"
}

variable "allow_public" {
  description = "If true, grants roles/run.invoker to allUsers (public API). MVP runs without auth, so this is the default."
  type        = bool
  default     = true
}

variable "database_url" {
  description = "JDBC URL for the application database (e.g. jdbc:postgresql://10.x.x.x:5432/browser_service)."
  type        = string
}

variable "database_username" {
  description = "Database username."
  type        = string
}

variable "database_password_secret_name" {
  description = "Short name of the Secret Manager secret that holds the DB password."
  type        = string
}

variable "selenium_grid_urls" {
  description = "List of Selenium grid URLs (each ending in /wd/hub). Joined with commas into SELENIUM_GRID_URLS."
  type        = list(string)
}

variable "extra_env" {
  description = "Additional plain (non-secret) environment variables to inject into the container."
  type        = map(string)
  default     = {}
}

variable "labels" {
  description = "Labels to apply to the Cloud Run service."
  type        = map(string)
  default     = {}
}
