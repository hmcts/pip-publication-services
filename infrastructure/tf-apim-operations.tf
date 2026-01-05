locals {
  operation_policies_files = fileset(path.module, "./resources/operation-policies/*.xml")
  operation_policies = local.deploy_apim == 0 ? {} : {
    for operation_policies_file in local.operation_policies_files :
    basename(operation_policies_file) => {
      operation_id = replace(basename(operation_policies_file), ".xml", "")
      xml_content = file("${path.module}/${operation_policies_file}")
    }
  }
}

resource "azurerm_api_management_api_operation_policy" "apim_api_operation_policy" {
  for_each            = { for operation in local.operation_policies : operation.operation_id => operation }
  operation_id        = each.value.operation_id
  api_name            = local.apim_api_name
  api_management_name = local.apim_name
  resource_group_name = local.apim_rg
  xml_content         = each.value.xml_content

  depends_on = [
    module.apim_api
  ]
}
