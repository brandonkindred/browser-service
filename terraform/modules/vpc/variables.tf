variable "project_id" {
  description = "GCP project ID."
  type        = string
}

variable "region" {
  description = "GCP region for the subnet and VPC connector."
  type        = string
}

variable "vpc_name" {
  description = "Name of the VPC network."
  type        = string
}

variable "subnet_name" {
  description = "Name of the primary subnet."
  type        = string
  default     = "browser-service-subnet"
}

variable "subnet_cidr" {
  description = "CIDR range for the primary subnet."
  type        = string
}

variable "connector_name" {
  description = "Name of the Serverless VPC Access connector. GCP caps this at 25 chars."
  type        = string
  default     = "bs-vpc-conn"

  validation {
    condition     = length(var.connector_name) <= 25
    error_message = "connector_name must be <= 25 characters (GCP Serverless VPC Access limit)."
  }
}

variable "connector_cidr" {
  description = "CIDR range for the Serverless VPC Access connector (must be a /28)."
  type        = string
  default     = "10.8.0.0/28"
}

variable "enable_nat" {
  description = "Whether to provision a Cloud Router + Cloud NAT for outbound internet egress. Required when any caller (e.g. browser-service) uses vpc-access-egress=all-traffic and needs to reach public non-Google endpoints."
  type        = bool
  default     = true
}
