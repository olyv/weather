terraform {
  required_version = ">= 1.3.0"
  required_providers {
    oci = {
      source  = "oracle/oci"
      version = ">= 5.0.0"
    }
  }
}

provider "oci" {
  config_file_profile = "DEFAULT"
}

# --- Variables ---
variable "compartment_ocid" {
  description = "The OCID of tenancy or compartment"
  type        = string
}

variable "ssh_public_key_path" {
  description = "path to SSH public key"
  type        = string
}

# --- Network Infrastructure ---
resource "oci_core_vcn" "weather_vcn" {
  compartment_id = var.compartment_ocid
  cidr_block     = "10.0.0.0/16"
  display_name   = "weather-iot-vcn"
}

resource "oci_core_internet_gateway" "igw" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.weather_vcn.id
  display_name   = "weather-iot-igw"
  enabled        = true
}

resource "oci_core_route_table" "public_rt" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.weather_vcn.id
  display_name   = "weather-iot-public-rt"

  route_rules {
    network_entity_id = oci_core_internet_gateway.igw.id
    destination       = "0.0.0.0/0"
  }
}

resource "oci_core_security_list" "weather_sec_list" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.weather_vcn.id
  display_name   = "weather-iot-security-list"

  # SSH Access
  ingress_security_rules {
    protocol    = "6" # TCP
    source      = "0.0.0.0/0"
    description = "Allow SSH"
    tcp_options {
      min = 22
      max = 22
    }
  }

  # MQTT Port for ESP32
  ingress_security_rules {
    protocol    = "6" # TCP
    source      = "0.0.0.0/0"
    description = "Allow MQTT Broker traffic from ESP32"
    tcp_options {
      min = 1883
      max = 1883
    }
  }

  egress_security_rules {
    protocol    = "all"
    destination = "0.0.0.0/0"
  }
}

resource "oci_core_subnet" "weather_subnet" {
  compartment_id    = var.compartment_ocid
  vcn_id            = oci_core_vcn.weather_vcn.id
  cidr_block        = "10.0.1.0/24"
  display_name      = "weather-iot-public-subnet"
  route_table_id    = oci_core_route_table.public_rt.id
  security_list_ids = [oci_core_security_list.weather_sec_list.id]
}

# --- Compute Data Sources ---
data "oci_identity_availability_domains" "ads" {
  compartment_id = var.compartment_ocid
}

data "oci_core_images" "oracle_linux_arm" {
  compartment_id           = var.compartment_ocid
  operating_system         = "Oracle Linux"
  operating_system_version = "9"
  shape                    = "VM.Standard.A1.Flex"
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"
}

# --- Compute Instance ---
resource "oci_core_instance" "weather_server" {
  compartment_id      = var.compartment_ocid
  availability_domain = data.oci_identity_availability_domains.ads.availability_domains[0].name
  shape               = "VM.Standard.A1.Flex"
  display_name        = "weather-iot-server"

  shape_config {
    ocpus         = 1
    memory_in_gbs = 6
  }

  create_vnic_details {
    subnet_id        = oci_core_subnet.weather_subnet.id
    assign_public_ip = true
  }

  source_details {
    source_type             = "image"
    source_id               = data.oci_core_images.oracle_linux_arm.images[0].id
    boot_volume_size_in_gbs = 50
  }

  metadata = {
    ssh_authorized_keys = file(pathexpand(var.ssh_public_key_path))
    # Cloud-Init script: Opens firewall ports on guest OS & installs Docker/Compose
    user_data = base64encode(<<-EOF
      #!/bin/bash
      sudo firewall-cmd --permanent --add-port=1883/tcp
      sudo firewall-cmd --reload
      sudo dnf-3 config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
      sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
      sudo systemctl enable --now docker
      sudo usermod -aG docker opc
    EOF
    )
  }
}

# --- Outputs ---
output "server_public_ip" {
  description = "Public IP Address of the Weather Server"
  value       = oci_core_instance.weather_server.public_ip
}