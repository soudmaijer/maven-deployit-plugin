# Setup your repository
1. Go to your Deployit Cli distribution
2. Go to lib directory : cd lib

3. Deploy deployit cli library into your repository: 
 * local repository : mvn install:install-file  -Dfile=cli-3.0.4.jar -DpomFile=dependencies/cli-3.0.4.pom
 * remote repository : mvn deploy:deploy-file  -Dfile=cli-3.0.4.jar -DpomFile=dependencies/cli-3.0.4.pom -url ...

4. Deploy deployit server-api library into your repository: 
 * local repository : mvn install:install-file  -Dfile=server-api-3.0.4.jar -DpomFile=dependencies/server-api-3.0.4.pom
 * remote repository : mvn deploy:deploy-file  -Dfile=server-api-3.0.4.jar -DpomFile=dependencies/server-api-3.0.4.pom -url ...


