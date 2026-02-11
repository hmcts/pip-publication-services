locals {
  apim_path      = "${var.product}/${var.component}"
  apim_api_name  = "${var.product}-${var.component}-api"
  api_policy_raw = file("./resources/api-policy/api-policy.xml")
  api_policy = replace(replace(replace(local.api_policy_raw,
    "{TENANT_ID}", data.azurerm_client_config.current.tenant_id),
    "{CLIENT_ID}", length(data.azurerm_key_vault_secret.data_client_id) > 0 ? data.azurerm_key_vault_secret.data_client_id[0].value : ""),
  "{ENV}", local.env)

  apim_api_name_testing_support  = "${var.product}-${var.component}-testing-support-api"
  api_policy_raw_testing_support = file("./resources/testing-support/api-policy/api-policy.xml")
  api_policy_testing_support = replace(replace(replace(local.api_policy_raw_testing_support,
    "{TENANT_ID}", data.azurerm_client_config.current.tenant_id),
    "{CLIENT_ID}", length(data.azurerm_key_vault_secret.data_client_id) > 0 ? data.azurerm_key_vault_secret.data_client_id[0].value : ""),
  "{ENV}", local.env)
}

module "apim_api" {
  count  = local.deploy_apim
  source = "git@github.com:hmcts/cnp-module-api-mgmt-api?ref=master"

  api_mgmt_name         = local.apim_name
  api_mgmt_rg           = local.apim_rg
  display_name          = local.apim_api_name
  name                  = local.apim_api_name
  path                  = local.apim_path
  product_id            = data.azurerm_api_management_product.apim_product[0].product_id
  protocols             = ["http", "https"]
  revision              = "1"
  service_url           = "https://${local.base_url}"
  swagger_url           = file("./resources/swagger/api-swagger.json")
  content_format        = "openapi+json"
  subscription_required = false
}

module "apim_api_policy" {
  count                  = local.deploy_apim
  source                 = "git@github.com:hmcts/cnp-module-api-mgmt-api-policy?ref=master"
  api_mgmt_name          = local.apim_name
  api_mgmt_rg            = local.apim_rg
  api_name               = local.apim_api_name
  api_policy_xml_content = local.api_policy

  depends_on = [
    module.apim_api
  ]
}

module "apim_api_testing_support" {
  count  = local.deploy_apim_testing_support
  source = "git@github.com:hmcts/cnp-module-api-mgmt-api?ref=master"

  api_mgmt_name         = local.apim_name
  api_mgmt_rg           = local.apim_rg
  display_name          = local.apim_api_name_testing_support
  name                  = local.apim_api_name_testing_support
  path                  = local.apim_path
  product_id            = data.azurerm_api_management_product.apim_product[0].product_id
  protocols             = ["http", "https"]
  revision              = "1"
  service_url           = "https://${local.base_url}"
  swagger_url           = file("./resources/testing-support/swagger/api-swagger.json")
  content_format        = "openapi+json"
  subscription_required = false
}

module "apim_api_policy_testing_support" {
  count                  = local.deploy_apim_testing_support
  source                 = "git@github.com:hmcts/cnp-module-api-mgmt-api-policy?ref=master"
  api_mgmt_name          = local.apim_name
  api_mgmt_rg            = local.apim_rg
  api_name               = local.apim_api_name_testing_support
  api_policy_xml_content = local.api_policy_testing_support

  depends_on = [
    module.apim_api_testing_support
  ]
}
