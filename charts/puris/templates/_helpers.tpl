{{- /*
* Copyright (c) 2022,2024 Volkswagen AG
* Copyright (c) 2022,2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
* Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
*
* See the NOTICE file(s) distributed with this work for additional
* information regarding copyright ownership.
*
* This program and the accompanying materials are made available under the
* terms of the Apache License, Version 2.0 which is available at
* https://www.apache.org/licenses/LICENSE-2.0.
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*
* SPDX-License-Identifier: Apache-2.0
*/}}
{{/*
Expand the name of the chart.
*/}}
{{- define "puris.backend.name" -}}
{{- $nameOverride := .Values.backend.nameOverride | default "" }}
{{- printf "%s-%sbackend" .Release.Name (ternary "" ($nameOverride | printf "%s-") (eq $nameOverride "")) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "puris.backend.fullname" -}}
{{- if .Values.backend.fullnameOverride }}
{{- .Values.backend.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name "backend" .Values.backend.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "puris.backend.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "puris.backend.labels" -}}
helm.sh/chart: {{ include "puris.backend.chart" . }}
{{ include "puris.backend.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}-backend
{{- end }}

{{/*
Selector labels
*/}}
{{- define "puris.backend.selectorLabels" -}}
app.kubernetes.io/name: {{ include "puris.backend.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}-backend
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "puris.backend.serviceAccountName" -}}
{{- if .Values.backend.serviceAccount.create }}
{{- default (include "puris.backend.fullname" .) .Values.backend.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.backend.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
FRONTEND
Expand the name of the chart.
*/}}
{{- define "puris.frontend.name" -}}
{{- $nameOverride := .Values.frontend.nameOverride | default "" }}
{{- printf "%s-%sfrontend" .Release.Name (ternary "" ($nameOverride | printf "%s-") (eq $nameOverride "")) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "puris.frontend.fullname" -}}
{{- if .Values.frontend.fullnameOverride }}
{{- .Values.frontend.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name "frontend" .Values.frontend.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "puris.frontend.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "puris.frontend.labels" -}}
helm.sh/chart: {{ include "puris.frontend.chart" . }}
{{ include "puris.frontend.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}-frontend
{{- end }}

{{/*
Selector labels
*/}}
{{- define "puris.frontend.selectorLabels" -}}
app.kubernetes.io/name: {{ include "puris.frontend.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}-frontend
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "puris.frontend.serviceAccountName" -}}
{{- if .Values.frontend.serviceAccount.create }}
{{- default (include "puris.frontend.fullname" .) .Values.frontend.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.frontend.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create a default fully qualified app name for PostgreSQL.
*/}}
{{- define "puris.postgresql.fullname" -}}
{{- if .Values.postgresql.fullnameOverride }}
{{- .Values.postgresql.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else if .Values.postgresql.nameOverride }}
{{- printf "%s-%s" .Release.Name .Values.postgresql.nameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-postgresql" .Release.Name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}


{{/*
Create a URL with the correct protocol prefix depending on wheter to apply TLS or not.
If http or https is already set, this is not changed BUT added if missing.
INPUTS:
- tlsConfig: configuration of the ingress for tls
- url: url to manipulate
*/}}
{{- define "getAsUrlWithProtocol" -}}
{{/* Get variables from dict*/}}
{{- $tlsConfig := .tlsConfig -}}
{{- $url := .url -}}
{{/* Get the correct prefix for the protocol*/}}
{{- $protocol := ternary "https://" "http://" (gt (len $tlsConfig) 0) -}}
{{/* Check if the URL does not include a protocol*/}}
{{- if not (or (hasPrefix "http://" $url) (hasPrefix "https://" $url)) -}}
{{- printf "%s%s" $protocol $url -}}
{{- else -}}
{{- printf "%s" $url -}} {{/* Fallback to just the url if protocl is included */}}
{{- end -}}
{{- end }}


{{- define "puris.backend.baseUrlWithProtocol" -}}
{{- $tlsConfig := .Values.backend.ingress.tls -}}
{{- $url := .Values.frontend.puris.baseUrl -}}
{{- $baseUrl := (include "getAsUrlWithProtocol" (dict "tlsConfig" $tlsConfig "url" $url) ) | trimSuffix "/" -}}
{{- printf "%s" $baseUrl }}
{{- end -}}
