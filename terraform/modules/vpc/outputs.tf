output "network_id" {
  description = "Self-link / id of the VPC network."
  value       = google_compute_network.vpc.id
}

output "network_name" {
  description = "Name of the VPC network."
  value       = google_compute_network.vpc.name
}

output "subnet_id" {
  description = "Self-link / id of the primary subnet."
  value       = google_compute_subnetwork.subnet.id
}

output "vpc_connector_id" {
  description = "Self-link of the Serverless VPC Access connector."
  value       = google_vpc_access_connector.connector.id
}

output "vpc_connector_name" {
  description = "Short name of the Serverless VPC Access connector (used in Cloud Run annotations)."
  value       = google_vpc_access_connector.connector.name
}

output "private_vpc_connection" {
  description = "Service networking connection (used as a depends_on target for Cloud SQL private IP)."
  value       = google_service_networking_connection.private_vpc_connection.id
}
