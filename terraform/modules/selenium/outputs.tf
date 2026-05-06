output "service_url" {
  description = "URL of the Selenium Cloud Run service."
  value       = google_cloud_run_service.selenium_standalone_chrome.status[0].url
}

output "service_name" {
  description = "Name of the Selenium Cloud Run service."
  value       = google_cloud_run_service.selenium_standalone_chrome.name
}

output "grid_url" {
  # The engine's BrowserConnectionHelper consumes SELENIUM_GRID_URLS entries
  # verbatim — they must be fully qualified URLs (scheme + host + path). Cloud
  # Run terminates TLS at the public *.run.app hostname on port 443, so the
  # resulting URL is `https://<host>/wd/hub`.
  description = "Fully qualified Selenium URL (scheme + host + /wd/hub) suitable for SELENIUM_GRID_URLS. Consumed verbatim by the engine."
  value       = "${google_cloud_run_service.selenium_standalone_chrome.status[0].url}/wd/hub"
}
