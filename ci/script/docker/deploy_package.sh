#!/usr/bin/env bash
set -e

if [ $# -eq 0 ]; then 
    echo "deploy snapshot package.."
    KIBANA_VERSION=6.5.4
    CMD="./mvnw clean verify -B -e && \\"
elif [ $# -eq 2 ]; then
    echo "deploy release package.."
    echo "plugin version: $1"
    echo "Kibana version: $2"
    VERSION=$1
    KIBANA_VERSION=$2
    CMD="./mvnw -Dtycho.mode=maven -DnewVersion=$VERSION -D-Pserver-distro org.eclipse.tycho:tycho-versions-plugin:set-version && \
         jq '.version=\"$VERSION\"' package.json > tmp && mv tmp package.json && \\"
else
    echo "Wrong number of parameters!"
    exit 2
fi

if [[ -z "${AWS_ACCESS_KEY_ID}" ]]; then
    echo "AWS_ACCESS_KEY_ID is undefined"
    exit 1
elif [[ -z "${AWS_SECRET_ACCESS_KEY}" ]]; then
    echo "AWS_SECRET_ACCESS_KEY is undefined"
    exit 1
fi

docker build --rm -f "ci/Dockerfile" --build-arg KIBANA_VERSION=$KIBANA_VERSION -t code-lsp-java-langserver-ci:latest ci

docker run \
    --rm -t $(tty &>/dev/null && echo "-i") \
    -e "AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}" \
    -e "AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}" \
    -v "$(pwd):/plugin/kibana-extra/java-langserver" \
    -v "$HOME/.m2":/root/.m2 \
    code-lsp-java-langserver-ci \
    /bin/bash -c "set -x && \
                  $CMD
                  ./mvnw -DskipTests=true clean deploy -DaltDeploymentRepository=dev::default::file:./repository -B -e -Pserver-distro && \
                  mv org.elastic.jdt.ls.product/distro/jdt-language-server* lib && \
                  yarn kbn bootstrap && echo $KIBANA_VERSION | yarn build && \
                  aws s3 cp build/java_languageserver-*.zip s3://download.elasticsearch.org/code/java-langserver/snapshot/"