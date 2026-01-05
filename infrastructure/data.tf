locals {
  resource_group_name = "${local.prefix}-${var.env}-rg"
  key_vault_name      = "${local.prefix}-kv-${var.env}"
}

data "azurerm_api_management" "sds_apim" {
  name                = local.apim_name
  resource_group_name = local.apim_rg
  depends_on = [
      module.apim_api
    ]
}

data "azurerm_api_management_product" "apim_product" {
  count               = local.deploy_apim
  product_id          = "${var.product}-product-${local.env}"
  resource_group_name = local.apim_rg
  api_management_name = local.apim_name
}

data "azurerm_client_config" "current" {}

data "azurerm_key_vault" "kv" {
  name                = local.key_vault_name
  resource_group_name = local.resource_group_name
}

data "azurerm_key_vault_secret" "data_client_id" {
  count        = local.deploy_apim
  name         = "app-pip-apim-admin-id"
  key_vault_id = data.azurerm_key_vault.kv.id
}
