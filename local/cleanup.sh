docker compose down -v
docker compose -f docker-compose-infrastructure.yaml down -v
docker image rm local-vault
