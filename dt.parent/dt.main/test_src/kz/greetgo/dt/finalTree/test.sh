#!/bin/sh

PWD=/home/greetgo/scripts/zookeeper_configure_single

echo Load configurations
ssh china-kafka1 cd $PWD \; perl load.pl
