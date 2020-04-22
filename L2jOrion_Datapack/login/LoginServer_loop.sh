#!/bin/bash

err=1
until [ $err == 0 ]; 
do
	[ -f log/java.log.0 ] && mv log/java.log.0 "log/`date +[%Y-%m-%d]_[%H:%M:%S]`_java.log"
    [ -f console.log ] &&  mv console.log "log/`date +[%Y-%m-%d]_[%H:%M:%S]`_console.log"
		
	java -Djava.util.logging.manager=l2jorion.util.L2LogManager -Xms128m -Xmx128m -cp ../libs/*:l2jorion-core.jar l2jorion.login.L2LoginServer > console.log 2>&1
	err=$?
#	/etc/init.d/mysql restart
	sleep 10;
done
