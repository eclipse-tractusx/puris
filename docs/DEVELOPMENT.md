## Keeping dependencies-files up to date
### Backend

Navigate to the `./backend` folder and run:  
```
mvn org.eclipse.dash:license-tool-plugin:license-check   
cp DEPENDENCIES ../DEPENDENCIES_BACKEND
```
The first line runs the maven license tool with the parameters specified in the 
`./backend/pom.xml` and produces a DEPENDENCIES file in the .`/backend` folder.  
Then this file gets copied to the PURIS-project root folder under the name `DEPENDENCIES_BACKEND`. 
Both files should be updated prior to any pull request.  

### Frontend
```
# move to a persistent folder. Could also be ~/jars.
mv org.eclipse.dash.licenses-1.0.2.jar ~/coding/org.eclipse.dash.licenses-1.0.2.jar
vim ~/.bashrc
# add following line using i
alias eclipseDashTool='java -jar ~/coding/org.eclipse.dash.licenses-1.0.2.jar'
# esc, qw -> enter to save and exit
source ~/.bashrc
# cd to puris-frontend
cd frontend
eclipseDashTool package-lock.json -project automotive.tractusx -summary ../DEPENDENCIES_FRONTEND
```

## Frontend container building workaround to use environment variables for vue

### The mechanism for docker is the following:
- `.env` has vite variables
- `.env.dockerbuild` has the vite variable that maps on an environment variable (`VITE_BACKEND_BASE_URL=$BACKEND_BASE_URL`)
- `src/config.json` has the environment variable names and the environment variable to substring in a json format.

### When building the container:
1. `.env.dockerbuild` is used
2. vite / vue builds the application into a dest folder, that will be served by nginx

> Result for the .env: <br> VITE_BACKEND_BASE_URL won't write a variable value BUT a placeholder into the built files ($BACKEND_BASE_URL)

### When building the container, there is a "start-nginx.sh" file that does the following
1. Collects the environment variables (set for the docker container / set via helm as `BACKEND_BASE_URL`)
2. Looks-up the "to replace string" from `config.json` (e.g., for `BACKEND_BASE_URL`, it will search for `$BACKEND_BASE-URL` in the built files)
3. Does the replacement in the built files
4. Starts nginx
