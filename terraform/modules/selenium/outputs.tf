output "service_url" {
  description = "URL of the Selenium Cloud Run service."
  value       = google_cloud_run_service.selenium_standalone_chrome.status[0].url
}

output "service_name" {
  description = "Name of the Selenium Cloud Run service."
  value       = google_cloud_run_service.selenium_standalone_chrome.name
}

output "grid_host" {
  # The engine's BrowserConnectionHelper#getConnection builds Selenium URLs as
  # `https://<entry>/wd/hub`, so SELENIUM_GRID_URLS entries must be bare
  # hostnames (no scheme, no path). Cloud Run terminates TLS at the public
  # *.run.app hostname on port 443, which matches the engine's hardcoded
  # `https://`.
  description = "Bare Selenium host (no scheme, no path) suitable for SELENIUM_GRID_URLS. The engine prepends https:// and appends /wd/hub at runtime."
  value       = trimprefix(google_cloud_run_service.selenium_standalone_chrome.status[0].url, "https://")
}
