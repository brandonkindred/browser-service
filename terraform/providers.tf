provider "google" {
  project     = var.project_id
  region      = var.region
  credentials = var.credentials_file != null ? file(var.credentials_file) : null
}

provider "google-beta" {
  project     = var.project_id
  region      = var.region
  credentials = var.credentials_file != null ? file(var.credentials_file) : null
}
