output "public_ip" {
  description = "Public IPv4 of the VM: ssh ubuntu@<this>."
  value       = oci_core_instance.vm.public_ip
}

output "image" {
  description = "The Ubuntu image the VM was created from."
  value       = data.oci_core_images.ubuntu.images[0].display_name
}
