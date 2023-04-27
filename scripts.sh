!# bash


# OPEN API 3

# BASH
docker run --rm -v $PWD:/local openapitools/openapi-generator-cli generate -i /local/api-docs.json -g java -o /local/generated/java

# CMD
docker run --rm -v %cd%:/local openapitools/openapi-generator-cli generate -i /local/api-docs.json -g java -o /local/speed-test-app/target/generated-sources

# POWERSHELL
docker run --rm -v ${PWD}:/local openapitools/openapi-generator-cli generate -i /local/api-docs.json -g java -o /local/generated/java




# DOCUMENTATION

npm i docsify-cli -g
docsify init ./docs
docsify serve docs