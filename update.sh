#!/bin/sh
echo '*********更新代码*********'

git checkout master
git fetch --all
git reset --hard origin/master
git pull
