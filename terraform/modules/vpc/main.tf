resource "google_compute_network" "vpc" {
  name                    = var.vpc_name
  description             = "Browser service VPC network"
  auto_create_subnetworks = false
  project                 = var.project_id
}

resource "google_compute_subnetwork" "subnet" {
  name                     = var.subnet_name
  ip_cidr_range            = var.subnet_cidr
  region                   = var.region
  network                  = google_compute_network.vpc.id
  project                  = var.project_id
  private_ip_google_access = true
}

# Dedicated /28 subnet for the Serverless VPC Access connector. We explicitly
# create the subnet (rather than letting the connector auto-create a hidden
# one via `ip_cidr_range`) because Cloud NAT cannot apply to hidden connector
# subnets — without this, the Cloud NAT below would never see connector
# traffic and `egress=all-traffic` workloads (the browser-service API) would
# have no outbound path to public non-Google endpoints.
resource "google_compute_subnetwork" "connector" {
  name          = "${var.connector_name}-subnet"
  ip_cidr_range = var.connector_cidr
  region        = var.region
  network       = google_compute_network.vpc.id
  project       = var.project_id
}

resource "google_vpc_access_connector" "connector" {
  name   = var.connector_name
  region = var.region

  subnet {
    name = google_compute_subnetwork.connector.name
  }

  min_instances = 2
  max_instances = 3
  machine_type  = "e2-micro"
  project       = var.project_id
}

# IAP SSH for compute instances tagged "iap-ssh" (matches LookseeIaC pattern,
# kept available for future debug VMs even though browser-service deploys no VMs).
resource "google_compute_firewall" "ssh_iap" {
  name    = "${var.vpc_name}-allow-iap-ssh"
  network = google_compute_network.vpc.name
  project = var.project_id

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  source_ranges = ["35.235.240.0/20"]
  target_tags   = ["iap-ssh"]
}

# Reserve a private IP range for service networking (used by Cloud SQL private IP).
resource "google_compute_global_address" "private_ip_range" {
  name          = "${var.vpc_name}-private-ip-range"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 16
  network       = google_compute_network.vpc.id
  project       = var.project_id
}

resource "google_service_networking_connection" "private_vpc_connection" {
  network                 = google_compute_network.vpc.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_ip_range.name]
}

# Cloud Router + Cloud NAT so traffic that leaves the VPC connector can reach
# the public internet. browser-service always runs with
# vpc-access-egress=all-traffic and reaches Selenium via public *.run.app
# URLs, so NAT is non-optional for this stack.
resource "google_compute_router" "router" {
  name    = "${var.vpc_name}-router"
  region  = var.region
  network = google_compute_network.vpc.id
  project = var.project_id
}

resource "google_compute_router_nat" "nat" {
  name                               = "${var.vpc_name}-nat"
  router                             = google_compute_router.router.name
  region                             = var.region
  project                            = var.project_id
  nat_ip_allocate_option             = "AUTO_ONLY"
  source_subnetwork_ip_ranges_to_nat = "ALL_SUBNETWORKS_ALL_IP_RANGES"

  log_config {
    enable = false
    filter = "ERRORS_ONLY"
  }
}
