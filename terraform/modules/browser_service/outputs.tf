output "service_url" {
  description = "Public URL of the browser-service API."
  value       = google_cloud_run_service.api.status[0].url
}

output "service_name" {
  description = "Cloud Run service name for the browser-service API."
  value       = google_cloud_run_service.api.name
}
