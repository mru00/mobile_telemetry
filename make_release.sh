#!/bin/bash -xeu

# Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc

make -C casing -j3


make -C firmware clean
make -C firmware build -j8


pushd app
bash gradlew clean
bash gradlew assemble
popd


mkdir -p release
rm release/*

cp firmware/target.hex release
cp app/MobileTelemetry/build/outputs/apk/*.apk release

