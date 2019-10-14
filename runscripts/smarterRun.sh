#!/bin/bash

SERVER=bftsmart.demo.microbenchmarks.ThroughputLatencyServer
#SERVER=bftsmart.demo.counter.CounterServer
OUTDIR="output/test"


#for i in {0..5}
for i in {0..3}
do
    java -Djava.security.properties="./config/java.security" -Dlogback.configurationFile="./config/logback.xml" -cp bin/*:lib/* $SERVER $i 100 120 10 0 "nosig" "rwd" &> $OUTDIR/$i.txt &
#    java -Djava.security.properties="./config/java.security" -Dlogback.configurationFile="./config/logback.xml" -cp bin/*:lib/* $SERVER $i 10 120 10 0 "nosig" "rwd" &> $OUTDIR/$i.txt &
#    java -Djava.security.properties="./config/java.security" -Dlogback.configurationFile="./config/logback.xml" -cp bin/*:lib/* $SERVER $i &> $OUTDIR/$i.txt &
done
