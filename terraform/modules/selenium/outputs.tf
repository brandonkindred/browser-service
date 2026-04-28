output "service_url" {
  description = "URL of the Selenium Cloud Run service."
  value       = google_cloud_run_service.selenium_standalone_chrome.status[0].url
}

output "service_name" {
  description = "Name of the Selenium Cloud Run service."
  value       = google_cloud_run_service.selenium_standalone_chrome.name
}

output "grid_endpoint" {
  description = "Selenium 4 grid endpoint (service URL + /wd/hub) ready for SELENIUM_GRID_URLS."
  value       = "${google_cloud_run_service.selenium_standalone_chrome.status[0].url}/wd/hub"
}
