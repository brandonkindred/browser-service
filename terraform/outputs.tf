output "browser_service_url" {
  description = "Public URL of the browser-service API."
  value       = module.browser_service.service_url
}

output "browser_service_name" {
  description = "Cloud Run service name for the browser-service API."
  value       = module.browser_service.service_name
}

output "selenium_grid_hosts" {
  description = "Bare Selenium hostnames (no scheme/path) injected into SELENIUM_GRID_URLS on the API. The engine prepends https:// and appends /wd/hub at runtime."
  value       = [for s in module.selenium : s.grid_host]
}

output "selenium_service_urls" {
  description = "Full Cloud Run URLs of each Selenium replica (for debugging — not what gets fed into the API env var)."
  value       = [for s in module.selenium : s.service_url]
}

output "selenium_service_names" {
  description = "Cloud Run service names for each Selenium replica."
  value       = [for s in module.selenium : s.service_name]
}

output "vpc_network" {
  description = "Name of the VPC the stack runs in."
  value       = module.vpc.network_name
}

output "vpc_connector_name" {
  description = "Serverless VPC Access connector name (used by both the API and Selenium replicas)."
  value       = module.vpc.vpc_connector_name
}

output "postgres_instance" {
  description = "Cloud SQL Postgres instance name."
  value       = module.postgres.instance_name
}

output "postgres_connection_name" {
  description = "Cloud SQL connection name (project:region:instance) for the Auth Proxy."
  value       = module.postgres.connection_name
}

output "postgres_private_ip" {
  description = "Private IP of the Postgres instance (only reachable from inside the VPC)."
  value       = module.postgres.private_ip_address
  sensitive   = true
}

output "postgres_password_secret" {
  description = "Secret Manager secret holding the generated DB password."
  value       = module.postgres.password_secret_name
}

output "runtime_service_account" {
  description = "Service account email used by Cloud Run revisions."
  value       = google_service_account.runtime.email
}
