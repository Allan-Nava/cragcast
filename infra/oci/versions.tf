terraform {
  required_version = ">= 1.6"
  required_providers {
    oci = {
      source  = "oracle/oci"
      version = "~> 7.0"
    }
  }
}

# Credentials come from ~/.oci/config (profile DEFAULT): user and tenancy OCIDs, the API key's
# fingerprint and the path to its private key. Nothing secret lives in this directory.
provider "oci" {
  config_file_profile = var.oci_profile
  region              = var.region
}
