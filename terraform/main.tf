# 网络资源：VPC + 交换机
resource "alicloud_vpc" "main" {
  vpc_name   = "taskflow-vpc"
  cidr_block = var.vpc_cidr
  tags       = var.common_tags
}

resource "alicloud_vswitch" "main" {
  vpc_id       = alicloud_vpc.main.id
  zone_id      = data.alicloud_zones.available.zones[0].id
  cidr_block   = var.subnet_cidr
  vswitch_name = "taskflow-subnet"
  tags         = var.common_tags
}

data "alicloud_zones" "available" {
  available_instance_type = var.instance_type
}

# 安全组：最小化开放端口
resource "alicloud_security_group" "main" {
  name        = "taskflow-sg"
  vpc_id      = alicloud_vpc.main.id
  description = "TaskFlow k3s 节点安全组"
  tags        = var.common_tags
}

resource "alicloud_security_group_rule" "ssh" {
  type              = "ingress"
  ip_protocol       = "tcp"
  port_range        = "22/22"
  security_group_id = alicloud_security_group.main.id
  cidr_ip           = "0.0.0.0/0" # 演示环境；生产请改为办公网出口 IP
  description       = "SSH"
}

resource "alicloud_security_group_rule" "k8s_web" {
  type              = "ingress"
  ip_protocol       = "tcp"
  port_range        = "80/6443"
  security_group_id = alicloud_security_group.main.id
  cidr_ip           = "0.0.0.0/0"
  description       = "HTTP / HTTPS / NodePort / k3s API"
}

resource "alicloud_security_group_rule" "icmp" {
  type              = "ingress"
  ip_protocol       = "icmp"
  port_range        = "-1/-1"
  security_group_id = alicloud_security_group.main.id
  cidr_ip           = "0.0.0.0/0"
  description       = "Ping"
}

# ECS 实例：user_data 自动安装 k3s（机器开机即可用）
resource "alicloud_instance" "k3s" {
  instance_name              = "taskflow-k3s"
  instance_type              = var.instance_type
  image_id                   = var.image_id
  security_groups            = [alicloud_security_group.main.id]
  vswitch_id                 = alicloud_vswitch.main.id
  internet_max_bandwidth_out = 10
  password                   = var.key_name == "" ? var.instance_password : null
  key_name                   = var.key_name == "" ? null : var.key_name

  user_data = <<-EOF
    #!/bin/bash
    set -euo pipefail
    export INSTALL_K3S_EXEC="--write-kubeconfig-mode 6443"
    curl -sfL https://get.k3s.io | sh -
    systemctl enable --now k3s
  EOF

  tags = var.common_tags
}
