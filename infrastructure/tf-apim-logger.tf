resource "azurerm_api_management_api_diagnostic" "api_logs" {
  count                    = local.deploy_apim
  identifier               = "applicationinsights"
  resource_group_name      = local.apim_rg
  api_management_name      = local.apim_name
  api_name                 = local.apim_api_name
  api_management_logger_id = "/subscriptions/${data.azurerm_client_config.current.subscription_id}/resourceGroups/${local.apim_rg}/providers/Microsoft.ApiManagement/service/${local.apim_name}/loggers/sds-api-mgmt-${local.env}-logger"

  sampling_percentage       = 25.0
  always_log_errors         = true
  log_client_ip             = true
  verbosity                 = "verbose"
  http_correlation_protocol = "W3C"

  frontend_request {
    body_bytes = 8192
    headers_to_log = [
      "content-type",
      "accept"
    ]
  }

  frontend_response {
    body_bytes = 8192
    headers_to_log = [
      "content-type",
      "content-length"
    ]
  }

  backend_request {
    body_bytes = 8192
    headers_to_log = [
      "content-type",
      "accept"
    ]
  }

  backend_response {
    body_bytes = 8192
    headers_to_log = [
      "content-type",
      "content-length"
    ]
  }
}
