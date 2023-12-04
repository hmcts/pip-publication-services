locals {
  env           = (var.env == "aat") ? "stg" : (var.env == "sandbox") ? "sbox" : "${(var.env == "perftest") ? "test" : "${var.env}"}"
  env_long_name = var.env == "sbox" ? "sandbox" : var.env == "stg" ? "staging" : var.env
  env_subdomain = local.env_long_name == "prod" ? "" : "${local.env_long_name}."

  base_url      = "${var.product}-${var.component}.${local.env_subdomain}platform.hmcts.net"
  prefix        = "${var.product}-ss"

  apim_name     = "sds-api-mgmt-${local.env}"
  apim_rg       = "ss-${local.env}-network-rg"

  deploy_apim   = local.env == "stg" ? 1 : 0
}
