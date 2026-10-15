> **Parked (2026-10-15).** The VM could not be created: Oracle answered "Out of host capacity"
> for A1.Flex in eu-milan-1 on all 39 tries over two days, on a free-tier account. The network
> was destroyed with `terraform destroy`; nothing is applied. Kept for a later attempt.

# infra/oci — the Always Free VM

Terraform for one Oracle Cloud **Always Free** Ampere VM in **eu-milan-1**, with its own
network: VCN, internet gateway, route, security list (22, 80, 443), public subnet. Variables
refuse anything above the free allowance (A1.Flex 2 OCPU / 12 GB, 200 GB of disk).

## Once

1. **API key.** The key pair is in `~/.oci/` (`oci_api_key.pem`, private, never shared;
   `oci_api_key_public.pem`). In the console: Profile → *User settings* → *Tokens and keys* →
   *Add API key* → *Paste a public key* → paste `oci_api_key_public.pem` → *Add*.
2. **`~/.oci/config`.** The console then shows a *Configuration file preview*: copy it into
   `~/.oci/config` and set `key_file=~/.oci/oci_api_key.pem`. `chmod 600 ~/.oci/config`.
3. **Variables.** `cp terraform.tfvars.example terraform.tfvars` and set `tenancy_ocid` (the
   same value as `tenancy=` in the config).

## Create the VM

```bash
terraform init
terraform plan          # check: 1 VM A1.Flex, "Always Free-eligible" sizes only
./retry-apply.sh        # apply, retrying every 2 min while Milan has no A1 capacity
terraform output public_ip
ssh ubuntu@$(terraform output -raw public_ip)
```

`terraform.tfvars`, `*.tfstate` and `.terraform/` are gitignored: the state holds OCIDs and
the public IP. Keep the state file safe — losing it means removing the resources by hand.
