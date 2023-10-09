## Keeping dependencies-files up to date
### Backend

Navigate to the ./backend folder and run:  
```
mvn org.eclipse.dash:license-tool-plugin:license-check   
cp DEPENDENCIES ../DEPENDENCIES_BACKEND
```
The first line runs the maven license tool with the parameters specified in the 
./backend/pom.xml and produces a DEPENDENCIES file in the ./backend folder.  
Then this file gets copied to the PURIS-project root folder under the name DEPENDENCIES_BACKEND. 
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
