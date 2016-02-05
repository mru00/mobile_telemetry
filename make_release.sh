#!/bin/bash -xeu

# Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc

pushd firmware
make clean
make build -j8
popd

pushd app
bash gradlew clean
bash gradlew assemble
popd


mkdir -p release
rm release/*

cp firmware/target.hex release
cp app/Application/build/outputs/apk/*.apk release

