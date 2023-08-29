# restarts the docker-compose and kill images, that may need a full refresh

docker-compose down

# kill vault container that has been built with given secrets
docker image rm local-vault

docker-compose up
