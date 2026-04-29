# Cloud Run service for a single Selenium standalone-chrome instance.
# Modeled directly on LookseeIaC/GCP/modules/selenium/main.tf so the deploy
# pattern (one Cloud Run service per Selenium replica, fronted by HTTPS) stays
# consistent across projects.
resource "google_cloud_run_service" "selenium_standalone_chrome" {
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
        "run.googleapis.com/vpc-access-egress"    = "private-ranges-only"
        "autoscaling.knative.dev/minScale"        = tostring(var.min_instances)
        "autoscaling.knative.dev/maxScale"        = tostring(var.max_instances)
      }
    }

    spec {
      service_account_name = var.service_account_email
      timeout_seconds      = var.request_timeout_seconds
      container_concurrency = var.container_concurrency

      containers {
        image = var.image

        ports {
          container_port = var.port
        }

        resources {
          limits = {
            memory = var.memory_allocation
            cpu    = var.cpu_allocation
          }
        }

        # Selenium 4 uses the /status endpoint for liveness, but Cloud Run
        # only requires the container to listen on $PORT — the standalone
        # image already does that on 4444. Extra env left as a hook.
        dynamic "env" {
          for_each = var.environment_variables
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
      # Cloud Run mutates these on its own (revisions, generated names);
      # skipping them avoids spurious diffs on every plan.
      metadata[0].annotations["run.googleapis.com/operation-id"],
      template[0].metadata[0].annotations["run.googleapis.com/operation-id"],
    ]
  }
}

# When ingress=internal we still need an IAM binding for the runtime SA to
# invoke peer Selenium services. Granting `allUsers` in combination with
# internal ingress means "anyone reachable on the VPC network", which is the
# pragmatic Selenium-grid pattern (Selenium speaks plain HTTP, not GCP auth).
resource "google_cloud_run_service_iam_member" "invoker" {
  for_each = toset(var.invoker_members)

  location = google_cloud_run_service.selenium_standalone_chrome.location
  project  = google_cloud_run_service.selenium_standalone_chrome.project
  service  = google_cloud_run_service.selenium_standalone_chrome.name
  role     = "roles/run.invoker"
  member   = each.value
}
