terraform {
  required_version = ">= 1.6"

  required_providers {
    alicloud = {
      source  = "aliyun/alicloud"
      version = "~> 1.95"
    }
  }
  # 建议配置 OSS 远程 state（团队协作必备）：
  # backend "oss" {
  #   bucket   = "your-tfstate-bucket"
  #   prefix   = "taskflow"
  #   region   = "cn-hangzhou"
  # }
}

provider "alicloud" {
  region = var.region
  # 凭据通过环境变量注入，切勿写入代码：
  #   export ALICLOUD_ACCESS_KEY=xxx
  #   export ALICLOUD_SECRET_KEY=xxx
}
