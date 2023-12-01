docker compose down -v
docker compose -f docker-compose-infrastructure.yaml down -v
docker image rm local-vault
rm .env
rm ./vault/secrets -r
echo "Deleted .env and vault/secrets"
