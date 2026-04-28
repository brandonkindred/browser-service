resource "random_password" "db" {
  length  = 32
  special = true
  # Cloud SQL accepts most special chars but exclude ones that complicate
  # JDBC URLs and shell quoting downstream.
  override_special = "-_=+"
}

resource "google_sql_database_instance" "this" {
  name                = "${var.instance_name}-${var.environment}"
  region              = var.region
  database_version    = var.database_version
  project             = var.project_id
  deletion_protection = var.deletion_protection

  depends_on = [var.private_vpc_connection]

  settings {
    tier              = var.tier
    disk_size         = var.disk_size_gb
    disk_type         = "PD_SSD"
    disk_autoresize   = true
    availability_type = var.availability_type

    backup_configuration {
      enabled                        = true
      point_in_time_recovery_enabled = true
      start_time                     = "03:00"
    }

    ip_configuration {
      ipv4_enabled                                  = false
      private_network                               = var.network_id
      enable_private_path_for_google_cloud_services = true
    }

    user_labels = var.labels
  }
}

resource "google_sql_database" "app" {
  name     = var.database_name
  instance = google_sql_database_instance.this.name
  project  = var.project_id
}

resource "google_sql_user" "app" {
  name     = var.database_user
  instance = google_sql_database_instance.this.name
  password = random_password.db.result
  project  = var.project_id
}

# Store the generated password in Secret Manager so the Cloud Run service can
# mount it as an env var without leaking it through Terraform state outputs.
resource "google_secret_manager_secret" "db_password" {
  secret_id = "${var.instance_name}-${var.environment}-db-password"
  project   = var.project_id

  replication {
    auto {}
  }

  labels = var.labels
}

resource "google_secret_manager_secret_version" "db_password" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = random_password.db.result
}

resource "google_secret_manager_secret_iam_member" "accessor" {
  secret_id = google_secret_manager_secret.db_password.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${var.service_account_email}"
  project   = var.project_id
}
