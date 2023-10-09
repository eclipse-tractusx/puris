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
