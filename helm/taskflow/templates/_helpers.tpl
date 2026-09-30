{{/*
通用命名与标签助手
*/}}
{{- define "taskflow.name" -}}
{{- default .Chart.Name .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "taskflow.labels" -}}
app.kubernetes.io/name: {{ include "taskflow.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/part-of: taskflow
{{- end -}}
