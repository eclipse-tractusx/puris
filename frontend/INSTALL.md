## Project Installation
The first steps are always the same:
1. Clone the project
2. Make sure the PURIS backend and the tractusx-edc is running with all its components

Depending on your needs of deployment, follow the following steps

### Running using npm (local develpment)
Use npm as follows:
3. The `VITE_BASE_URL` in the `.env` file must be configured to the PURIS backend instance.
4. Run the application:
```shell
npm install

# A development
npm run dev

# B production
npm run build
```
5. Done! The frontend should be available at `http://YOURIP:3000/`

### Running using docker (deployment)
As JS-frameworks hard-wire their environments, this project uses a workaround to make the .env variables replaceable by (docker) environment variables.
3. Build and run the docker frontend as follows:
```shell
cd frontend

docker build -t puris-frontend:dev .

# A use docker 
docker run -d --rm -p 3000:8080 --name frontend -e BACKEND_BASE_URL=http://YOURBACKENDIP:8081/catena puris-frontend:dev CONTAINERID

# B use docker-compose
docker-compose up
```
Note: please find the available parameters in src/config.json
4. Done! The frontend should be available at `http://YOURIP:3000/`

### Running using helm (deployment)
3. Run the application:
```shell
cd charts/puris/charts/frontend

helm install frontend --namespace puris --create-namespace . --set ingress.enabled=true --values ../../values.yaml
```
4. Done! The frontend should be available at `http://YOURIP:30000/`
