#!/bin/bash

SERVER=bftsmart.demo.counter.CounterServer
OUTDIR="output/test"


for i in {0..3}
do
    java -Djava.security.properties="./config/java.security" -Dlogback.configurationFile="./config/logback.xml" -cp bin/*:lib/* $SERVER $i &> $OUTDIR/$i.txt &
done
