# Migration of Chart 7.0.0 - Cloudpirates

> [! prerequisite]
>
> You need argocd cli installed.
>

```shell
argocd login argocd.int.catena-x.net --sso
```

Overall Flow:

1. Scale down application
2. Create migration-pvc and backup data
3. Install new Chart
4. Restore backup
5. Scale up

## Migrate PURIS (Supplier)

### Prepare per Service

Download folder `template`. Go to its parent dir.

If not already done, create a branch on github in eclipse-tractus-x

```bash
export GIT_REPO=https://github.com/eclipse-tractusx/puris
export GIT_BRANCH=feat/consortia-int-values
git checkout -b $GIT_BRANCH

```

Now refine these variables to derive your custom templates for the migration. We'll use environment substitution to create further ones.

```bash
export ARGO_PROJECT=project-puris
export NS=product-puris
export APPLICATION=supplier-puris
export PG_VERSION_FROM=15
export PG_VERSION_TO=18
export PG_ADMIN_USER=postgres
export PG_SERVICE_NAME=supplier-backend-postgresql
export PG_DAB=postgres
export PG_POSTGRES_PW_SECRET=supplier-secret-postgres-init
export PG_POSTGRES_PW_SECRET_KEY=postgres-password
```

```bash
export ARGO_PROJECT=project-puris
export NS=product-puris
export APPLICATION=supplier-edc
export PG_VERSION_FROM=16
export PG_VERSION_TO=18
export PG_ADMIN_USER=postgres
export PG_SERVICE_NAME=supplier-edc-postgresql
export PG_DB=edc
export PG_POSTGRES_PW_SECRET=supplier-edc-postgresql
export PG_POSTGRES_PW_SECRET_KEY=postgres-password
```


```bash
export ARGO_PROJECT=project-puris
export NS=product-puris
export APPLICATION=customer-edc
export PG_VERSION_FROM=16
export PG_VERSION_TO=18
export PG_ADMIN_USER=postgres
export PG_SERVICE_NAME=customer-edc-postgresql
export PG_DB=edc
export PG_POSTGRES_PW_SECRET=customer-edc-postgresql
export PG_POSTGRES_PW_SECRET_KEY=postgres-password
```

- [ ] TODO DTR supplier

Now do environment substitution:

```bash
echo "...creating manifests for backup..."
echo "...creating folders..."
mkdir -p $APPLICATION/01-backup
mkdir -p $APPLICATION/02-restore

echo "...creating manifests..."
envsubst < ./template/01-backup/01-migration-pvc.yaml > $APPLICATION/01-backup/01-migration-pvc.yaml
envsubst < ./template/01-backup/02-backup.yaml > $APPLICATION/01-backup/02-backup.yaml

cp ./$APPLICATION/01-backup/01-migration-pvc.yaml ./$APPLICATION/02-restore/01-migration-pvc.yaml
envsubst < ./template/02-restore/03-restore.yaml > $APPLICATION/02-restore/03-restore.yaml
```

Commit the changes to your branch

```bash
git add ./$APPLICATION/.
git commit -m "feat($APPLICATION): add postgres migration sources for argocd"
git push
```

### 1. Scale down applications

Update your deployment to downscale the deployment. For puris at least the backend needs to be downscaled.

```yaml
backend:
    autoscaling:
        enabled: false
    replicaCount: 0
```

Same should be done for all other backend applications

DTR with keycloak

```yaml
registry:
    replicaCount: 0
keycloak:
    autoscaling:
        enabled: false
    replicaCount: 0
```

EDC

```yaml
controlplane:
    autoscaling:
        enabled: false
    replicaCount: 0
dataplane:
    autoscaling:
        enabled: false
    replicaCount: 0
```

Commit, push, sync

```shell

argocd app get supplier-edc --hard-refresh
argocd app get supplier-dtr --hard-refresh
argocd app get supplier-puris --hard-refresh
argocd app sync supplier-edc
argocd app sync supplier-dtr
argocd app sync supplier-puris
```

### 2. Create migration-pvc and backup data

Create migration pvc and backup data

```shell
argocd app create db-migration-$APPLICATION \
  --project project-puris \
  --repo https://github.com/eclipse-tractusx/puris \
  --revision $GIT_BRANCH \
  --path environments/association/int/migration/7.0.0/$APPLICATION/01-backup \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace $NS \
  --directory-recurse

# Sync to create the PVC and run the Backup Job
argocd app sync db-migration-$APPLICATION

# Check status and logs
argocd app wait db-migration-$APPLICATION
argocd app logs db-migration-$APPLICATION | grep error
```

> [!tip]
> If you lost your postgres password you can temporarily allow full local access to change the postgresPassword.
>
> 1. Configure `pgHbaConfiguration` & sync
> 2. Update password via pod exec
> 3. Remove `pgHbaConfiguration` & sync

Configuration to be added / removed and synced

```yaml
postgresql:
  primary:
    pgHbaConfiguration: |-
      local all all trust
      host all all all md5 
```

Exec into the pod of the stateful set.

```shell
psql -U postgres
ALTER USER postgres WITH PASSWORD 'TODO';
COMMIT;
\q
```

###  3. Install new chart

Update the manifest temporarily (UI):

- fork url with feature branch of postgres update
- overwrite current image versions in [./supplier/puris.yaml](./supplier/puris.yaml)

```yaml
frontend:
    image:
        tag: "5.1.1"
backend:
    image:
        tag: "5.1.1"
```

> [! warning]
> 
> Before these steps ensure that the dump succeeded.
> 

To retain the old statefulSet and persistentVolumeClaim names, do the following:

- delete the old postgres pvc (only possible via UI)
- delete the old statefulSet
- delete the old services
- delete the old headless service

```shell
# to check if this step can be done via orphanage of the statefulSet, depends on retain policy
argocd app delete-resource supplier-puris \
  --kind PersistentVolumeClaim \
  --resource-name data-supplier-backend-postgresql-0 \
  --namespace product-puris

argocd app delete-resource supplier-puris \
  --kind StatefulSet \
  --resource-name supplier-backend-postgresql \
  --namespace product-puris

argocd app delete-resource supplier-puris \
  --kind Service \
  --resource-name supplier-backend-postgresql \
  --namespace product-puris

argocd app delete-resource supplier-puris \
  --kind Service \
  --resource-name supplier-backend-postgresql-hl \
  --namespace product-puris
```

Then sync

```shell
argocd app get supplier-puris --hard-refresh
argocd app sync supplier-puris
```

### 4. Restore Backup

```shell

argocd app create db-migration-puris-supplier \
  --project project-puris \
  --repo https://github.com/eclipse-tractusx/puris \
  --revision feat/consortia-int-values \
  --path environments/association/int/migration/7.0.0/supplier/puris/02-restore \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace product-puris \
  --directory-recurse \
  --upsert

# Sync to create the PVC and run the Backup Job
argocd app sync db-migration-puris-supplier

```


### 5. Scale up Applications

```yaml
backend:
    autoscaling:
        enabled: false
    replicaCount: 1
```

Same should be done for all other backend applications

DTR with keycloak

```yaml
registry:
    replicaCount: 0
keycloak:
    autoscaling:
        enabled: false
    replicaCount: 0
```

EDC

```yaml
controlplane:
    autoscaling:
        enabled: false
    replicaCount: 0
dataplane:
    autoscaling:
        enabled: false
    replicaCount: 0
```

Commit, push, sync

```shell

argocd app get supplier-edc --hard-refresh
argocd app get supplier-dtr --hard-refresh
argocd app get supplier-puris --hard-refresh
argocd app sync supplier-edc
argocd app sync supplier-dtr
argocd app sync supplier-puris
```