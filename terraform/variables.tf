variable "region" {
  description = "阿里云地域"
  type        = string
  default     = "cn-hangzhou"
}

variable "instance_type" {
  description = "ECS 实例规格（k3s 单节点建议 ≥ 2C4G）"
  type        = string
  default     = "ecs.e-c1m2.large"
}

variable "image_id" {
  description = "操作系统镜像（Ubuntu 22.04）"
  type        = string
  default     = "ubuntu_22_04_x64_20G_alibase_20240220.vhd"
}

variable "vpc_cidr" {
  description = "VPC 网段"
  type        = string
  default     = "172.16.0.0/16"
}

variable "subnet_cidr" {
  description = "交换机网段"
  type        = string
  default     = "172.16.0.0/24"
}

variable "key_name" {
  description = "已存在的 SSH 密钥对名称（优先于密码登录）"
  type        = string
  default     = ""
}

variable "instance_password" {
  description = "root 登录密码（仅演示用；生产请使用 key_name 或 KMS）"
  type        = string
  sensitive   = true
  default     = ""
}

variable "common_tags" {
  description = "资源统一打标（成本分摊与规范管理）"
  type        = map(string)
  default = {
    Project   = "taskflow"
    ManagedBy = "terraform"
  }
}
