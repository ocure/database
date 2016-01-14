#!/bin/bash

FILE_OR_DIR=$1

if [ -f "/etc/default/blazegraph" ] ; then
    . "/etc/default/blazegraph" 
else
    JETTY_PORT=9999
fi

LOAD_PROP_FILE=/tmp/$$.properties

export NSS_DATALOAD_PROPERTIES=/usr/local/blazegraph/conf/RWStore.properties

#Probably some unused properties below, but copied all to be safe.

cat <<EOT >> $LOAD_PROP_FILE
quiet=false
verbose=2
closure=false
durableQueues=false
com.bigdata.journal.AbstractJournal.writeCacheBufferCount=2000
com.bigdata.journal.AbstractJournal.writeCacheMinCleanListSize=50
com.bigdata.rdf.store.DataLoader.flush=false
com.bigdata.rdf.store.DataLoader.bufferCapacity=100000
com.bigdata.rdf.store.DataLoader.queueCapacity=10
com.bigdata.btree.writeRetentionQueue.capacity=8000
com.bigdata.io.DirectBufferPool.bufferCapacity=1048576
com.bigdata.btree.proc.AbstractKeyArrayIndexProcedure.maxReaders=0
com.bigdata.btree.proc.AbstractKeyArrayIndexProcedure.skipCount=256
com.bigdata.btree.proc.AbstractKeyArrayIndexProcedure.spannedRangeMultiplier=10
com.bigdata.btree.proc.AbstractKeyArrayIndexProcedure.batchSize=10240
com.bigdata.btree.proc.AbstractKeyArrayIndexProcedure.queueCapacity=40
#Namespace to load
namespace=kb
#Files to load
fileOrDirs=$1
#Property file (if creating a new namespace)
propertyFile=$NSS_DATALOAD_PROPERTIES
EOT

echo "Loading with properties..."

cat $LOAD_PROP_FILE

curl -X POST --data-binary @${LOAD_PROP_FILE} --header 'Content-Type:text/plain' http://localhost:${JETTY_PORT}/blazegraph/dataloader

#Let the output go to STDOUT/ERR to allow script redirection

rm -f $LOAD_PROP_FILE