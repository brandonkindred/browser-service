locals {
  base_labels = merge(
    {
      app         = "browser-service"
      environment = var.environment
      managed-by  = "terraform"
    },
    var.labels,
  )
}

# Enable APIs the stack depends on. Idempotent; safe to leave on.
resource "google_project_service" "required" {
  for_each = toset([
    "compute.googleapis.com",
    "run.googleapis.com",
    "vpcaccess.googleapis.com",
    "sqladmin.googleapis.com",
    "secretmanager.googleapis.com",
    "servicenetworking.googleapis.com",
    "iam.googleapis.com",
  ])

  project            = var.project_id
  service            = each.value
  disable_on_destroy = false
}

# Single runtime service account used by the browser-service API and the
# Selenium Cloud Run replicas. Keeping it shared simplifies IAM since the API
# already needs to invoke each Selenium peer.
resource "google_service_account" "runtime" {
  project      = var.project_id
  account_id   = "browser-service-${var.environment}"
  display_name = "Browser Service runtime (${var.environment})"

  depends_on = [google_project_service.required]
}

module "vpc" {
  source = "./modules/vpc"

  project_id  = var.project_id
  region      = var.region
  vpc_name    = var.vpc_name
  subnet_cidr = var.subnet_cidr

  depends_on = [google_project_service.required]
}

module "postgres" {
  source = "./modules/postgres"

  project_id             = var.project_id
  region                 = var.region
  environment            = var.environment
  network_id             = module.vpc.network_id
  private_vpc_connection = module.vpc.private_vpc_connection
  database_name          = var.postgres_database_name
  database_user          = var.postgres_user
  database_version       = var.postgres_version
  tier                   = var.postgres_tier
  disk_size_gb           = var.postgres_disk_size_gb
  deletion_protection    = var.postgres_deletion_protection
  service_account_email  = google_service_account.runtime.email
  labels                 = local.base_labels
}

# Fan out N Selenium standalone-chrome Cloud Run services. Each is the same
# pattern as LookseeIaC/GCP/modules/selenium, just provisioned `count` times
# with a stable per-replica name.
module "selenium" {
  source = "./modules/selenium"
  count  = var.selenium_instance_count

  project_id            = var.project_id
  region                = var.region
  environment           = var.environment
  service_name          = "selenium-chrome-${var.environment}-${format("%02d", count.index)}"
  image                 = var.selenium_image
  service_account_email = google_service_account.runtime.email
  vpc_connector_name    = module.vpc.vpc_connector_name
  port                  = var.selenium_port
  memory_allocation     = var.selenium_memory_allocation
  cpu_allocation        = var.selenium_cpu_allocation
  ingress = "internal"
  # Selenium speaks plain HTTP, not GCP id-token auth, so invoker auth would
  # block the Java client. Pair allUsers with ingress=internal: the IAM check
  # passes once traffic is on the VPC, and ingress=internal keeps the public
  # internet out.
  invoker_members = ["allUsers"]
  labels          = local.base_labels
}

module "browser_service" {
  source = "./modules/browser_service"

  project_id            = var.project_id
  region                = var.region
  service_name          = "${var.browser_service_service_name}-${var.environment}"
  image                 = var.browser_service_image
  service_account_email = google_service_account.runtime.email
  vpc_connector_name    = module.vpc.vpc_connector_name
  min_instances         = var.browser_service_min_instances
  max_instances         = var.browser_service_max_instances
  memory                = var.browser_service_memory
  cpu                   = var.browser_service_cpu
  allow_public          = var.browser_service_allow_public

  database_url                  = "jdbc:postgresql://${module.postgres.private_ip_address}:5432/${module.postgres.database_name}"
  database_username             = module.postgres.database_user
  database_password_secret_name = module.postgres.password_secret_name

  selenium_grid_urls = [for s in module.selenium : s.grid_endpoint]

  labels = local.base_labels

  # Make sure secret access IAM is in place before the service tries to read it.
  depends_on = [module.postgres]
}
