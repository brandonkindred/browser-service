output "instance_name" {
  description = "Cloud SQL instance name."
  value       = google_sql_database_instance.this.name
}

output "connection_name" {
  description = "Cloud SQL connection name (project:region:instance)."
  value       = google_sql_database_instance.this.connection_name
}

output "private_ip_address" {
  description = "Private IP address of the Cloud SQL instance, reachable from the VPC connector."
  value       = google_sql_database_instance.this.private_ip_address
}

output "database_name" {
  description = "Application database name."
  value       = google_sql_database.app.name
}

output "database_user" {
  description = "Application database user."
  value       = google_sql_user.app.name
}

output "password_secret_name" {
  description = "Short name of the Secret Manager secret holding the DB password."
  value       = google_secret_manager_secret.db_password.secret_id
}

output "password_secret_id" {
  description = "Full resource id of the Secret Manager secret holding the DB password."
  value       = google_secret_manager_secret.db_password.id
}

output "password_secret_version" {
  description = "Specific version id of the Secret Manager secret created in this run."
  value       = google_secret_manager_secret_version.db_password.id
}
