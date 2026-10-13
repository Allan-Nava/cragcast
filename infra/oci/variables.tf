variable "tenancy_ocid" {
  description = "OCID of the tenancy; the root compartment, where everything is created."
  type        = string
}

variable "oci_profile" {
  description = "Profile in ~/.oci/config."
  type        = string
  default     = "DEFAULT"
}

variable "region" {
  description = "Home region: Always Free resources exist only there."
  type        = string
  default     = "eu-milan-1"
}

variable "ssh_public_key_path" {
  description = "Public key allowed to log in as ubuntu."
  type        = string
  default     = "~/.ssh/id_ed25519.pub"
}

# Always Free since 2026-08-18: A1.Flex 2 OCPU and 12 GB in total for the tenancy. The
# validations stop a plan that would start billing.
variable "ocpus" {
  description = "OCPUs of the A1.Flex VM. 1 is easier to get when Milan is short of capacity."
  type        = number
  default     = 1
  validation {
    condition     = var.ocpus >= 1 && var.ocpus <= 2
    error_message = "Always Free allows at most 2 OCPUs of A1.Flex in total."
  }
}

variable "memory_gb" {
  description = "Memory of the A1.Flex VM, in GB."
  type        = number
  default     = 12
  validation {
    condition     = var.memory_gb >= 1 && var.memory_gb <= 12
    error_message = "Always Free allows at most 12 GB of A1.Flex memory in total."
  }
}

variable "boot_volume_gb" {
  description = "Boot volume size, in GB."
  type        = number
  default     = 100
  validation {
    condition     = var.boot_volume_gb >= 50 && var.boot_volume_gb <= 200
    error_message = "Always Free block storage is 200 GB in total, boot volumes included."
  }
}

variable "ssh_source_cidr" {
  description = "Who may reach SSH (22). Narrow it to your own IP/32 when you can."
  type        = string
  default     = "0.0.0.0/0"
}
