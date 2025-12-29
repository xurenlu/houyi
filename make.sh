  mvn clean package -DskipTests=true
  docker rmi wework
  docker build -t wework .
  docker run -p  7070:8080 -ti -v `pwd`/target/:/app/java/ --rm --name wework-msgaudit wework
