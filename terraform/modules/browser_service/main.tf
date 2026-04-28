resource "google_cloud_run_service" "api" {
  name     = var.service_name
  location = var.region
  project  = var.project_id

  metadata {
    annotations = {
      "run.googleapis.com/ingress"     = var.ingress
      "run.googleapis.com/client-name" = "terraform"
    }
    labels = var.labels
  }

  template {
    metadata {
      annotations = {
        "run.googleapis.com/vpc-access-connector" = var.vpc_connector_name
        # all-traffic so calls to internal-ingress Selenium services traverse
        # the VPC (otherwise the public *.run.app URL is reached over the
        # internet and rejected by ingress=internal).
        "run.googleapis.com/vpc-access-egress" = "all-traffic"
        "autoscaling.knative.dev/minScale"        = tostring(var.min_instances)
        "autoscaling.knative.dev/maxScale"        = tostring(var.max_instances)
      }
    }

    spec {
      service_account_name  = var.service_account_email
      timeout_seconds       = var.request_timeout_seconds
      container_concurrency = var.container_concurrency

      containers {
        image = var.image

        ports {
          container_port = var.port
        }

        resources {
          limits = {
            memory = var.memory
            cpu    = var.cpu
          }
        }

        env {
          name  = "DATABASE_URL"
          value = var.database_url
        }

        env {
          name  = "DATABASE_USERNAME"
          value = var.database_username
        }

        env {
          name = "DATABASE_PASSWORD"
          value_from {
            secret_key_ref {
              name = var.database_password_secret_name
              key  = "latest"
            }
          }
        }

        env {
          name  = "SELENIUM_GRID_URLS"
          value = join(",", var.selenium_grid_urls)
        }

        dynamic "env" {
          for_each = var.extra_env
          content {
            name  = env.key
            value = env.value
          }
        }
      }
    }
  }

  traffic {
    percent         = 100
    latest_revision = true
  }

  lifecycle {
    ignore_changes = [
      metadata[0].annotations["run.googleapis.com/operation-id"],
      template[0].metadata[0].annotations["run.googleapis.com/operation-id"],
    ]
  }
}

resource "google_cloud_run_service_iam_member" "public" {
  count = var.allow_public ? 1 : 0

  location = google_cloud_run_service.api.location
  project  = google_cloud_run_service.api.project
  service  = google_cloud_run_service.api.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}
