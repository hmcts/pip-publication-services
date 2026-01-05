resource "azurerm_api_management_policy_fragment" "jwt-validation" {
  api_management_id = data.azurerm_api_management_product.apim_product.id
  name              = "${var.product}-jwt-validation"
  format            = "rawxml"
  description       = "This fragment validate input JWT token"
  value             = file("${path.module}/resources/policy-fragments/jwt-validation-fragment.xml")
}
