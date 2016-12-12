#!/usr/bin/env bash

rm -rf deploy/target
rm -rf deploy/resources/public/site-min
lein cljsbuild once server site-min
rsync -aLv deploy/ wilkerlucio@daverees4.webfactional.com:/home/daverees4/webapps/dc_beta/site
