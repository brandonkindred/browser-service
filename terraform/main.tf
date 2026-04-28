locals {
  base_labels = merge(
    {
      app         = "browser-service"
      environment = var.environment
      managed-by  = "terraform"
    },
    var.labels,
  )

  # Default network names include `environment` so multiple stacks can share a
  # project without collisions. tfvars can still override any of these.
  vpc_name           = coalesce(var.vpc_name, "browser-service-vpc-${var.environment}")
  subnet_name        = coalesce(var.subnet_name, "browser-service-subnet-${var.environment}")
  # Serverless VPC Access connector names are capped at 25 chars, so this
  # default uses a short prefix. The `environment` variable is validated at
  # 12 chars, which keeps `bs-conn-<env>` (8 + env) safely under the limit.
  vpc_connector_name = coalesce(var.vpc_connector_name, "bs-conn-${var.environment}")
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
    "artifactregistry.googleapis.com",
  ])

  project            = var.project_id
  service            = each.value
  disable_on_destroy = false
}

# Two runtime service accounts so least-privilege holds: only the API SA
# needs Secret Manager access for the DB password (granted by the postgres
# module), while Selenium replicas run with a separate identity that has no
# privileged role bindings. If a Selenium container is compromised, DB
# credentials don't come along for free.
resource "google_service_account" "api_runtime" {
  project      = var.project_id
  account_id   = "bs-api-${var.environment}"
  display_name = "Browser Service API runtime (${var.environment})"

  depends_on = [google_project_service.required]
}

resource "google_service_account" "selenium_runtime" {
  project      = var.project_id
  account_id   = "bs-selenium-${var.environment}"
  display_name = "Browser Service Selenium runtime (${var.environment})"

  depends_on = [google_project_service.required]
}

module "vpc" {
  source = "./modules/vpc"

  project_id     = var.project_id
  region         = var.region
  vpc_name       = local.vpc_name
  subnet_name    = local.subnet_name
  connector_name = local.vpc_connector_name
  subnet_cidr    = var.subnet_cidr
  enable_nat     = var.vpc_enable_nat

  depends_on = [google_project_service.required]
}

module "postgres" {
  source = "./modules/postgres"

  project_id            = var.project_id
  region                = var.region
  environment           = var.environment
  network_id            = module.vpc.network_id
  database_name         = var.postgres_database_name
  database_user         = var.postgres_user
  database_version      = var.postgres_version
  tier                  = var.postgres_tier
  disk_size_gb          = var.postgres_disk_size_gb
  deletion_protection   = var.postgres_deletion_protection
  service_account_email = google_service_account.api_runtime.email
  labels                = local.base_labels

  # The Cloud SQL instance can't be created until private services access
  # is established on the VPC. Depending on the whole module (rather than
  # threading the service-networking-connection id through a variable)
  # keeps the dependency a real Terraform graph edge.
  depends_on = [module.vpc]
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
  service_account_email = google_service_account.selenium_runtime.email
  vpc_connector_name    = module.vpc.vpc_connector_name
  port                  = var.selenium_port
  memory_allocation     = var.selenium_memory_allocation
  cpu_allocation        = var.selenium_cpu_allocation
  min_instances         = var.selenium_min_instances
  max_instances         = var.selenium_max_instances
  ingress               = "internal"
  # Selenium speaks plain HTTP, not GCP id-token auth, so invoker auth would
  # block the Java client. The default `["allUsers"]` paired with
  # ingress=internal keeps the public internet out while letting in-VPC
  # callers reach the service. In orgs that block allUsers via
  # iam.allowedPolicyMemberDomains, set selenium_invoker_members = [].
  invoker_members = var.selenium_invoker_members
  labels          = local.base_labels
}

module "browser_service" {
  source = "./modules/browser_service"

  project_id            = var.project_id
  region                = var.region
  service_name          = "${var.browser_service_service_name}-${var.environment}"
  image                 = var.browser_service_image
  service_account_email = google_service_account.api_runtime.email
  vpc_connector_name    = module.vpc.vpc_connector_name
  min_instances         = var.browser_service_min_instances
  max_instances         = var.browser_service_max_instances
  memory                = var.browser_service_memory
  cpu                   = var.browser_service_cpu
  allow_public          = var.browser_service_allow_public

  database_url                  = "jdbc:postgresql://${module.postgres.private_ip_address}:5432/${module.postgres.database_name}"
  database_username             = module.postgres.database_user
  database_password_secret_name = module.postgres.password_secret_name

  selenium_grid_urls = [for s in module.selenium : s.grid_host]

  labels = local.base_labels

  # Make sure secret access IAM is in place before the service tries to read it.
  depends_on = [module.postgres]
}
