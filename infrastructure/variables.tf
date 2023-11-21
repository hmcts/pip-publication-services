variable "product" {
  default = "pip"
}

variable "component" {
  default = "publication-services"
}

variable "location" {
  default = "UK South"
}

variable "env" {}

variable "common_tags" {
  type = map(string)
}
