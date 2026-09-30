output "public_ip" {
  description = "k3s 节点公网 IP"
  value       = alicloud_instance.k3s.public_ip
}

output "ssh_command" {
  description = "登录命令"
  value       = "ssh root@${alicloud_instance.k3s.public_ip}"
}

output "kubeconfig_hint" {
  description = "节点上获取 kubeconfig 的方式"
  value       = "ssh root@${alicloud_instance.k3s.public_ip} 'cat /etc/rancher/k3s/k3s.yaml'"
}
