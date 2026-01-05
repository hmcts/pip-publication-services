resource "azurerm_api_management_policy_fragment" "jwt-validation" {
  api_management_id = data.azurerm_api_management.sds_apim.id
  name              = "${var.product}-jwt-validation"
  format            = "rawxml"
  description       = "This fragment validate input JWT token"
  value = replace(replace(file("${path.module}/resources/policy-fragments/jwt-validation-fragment.xml"),
    "{TENANT_ID}", data.azurerm_client_config.current.tenant_id),
  "{CLIENT_ID}", length(data.azurerm_key_vault_secret.data_client_id) > 0 ? data.azurerm_key_vault_secret.data_client_id[0].value : "")
}
