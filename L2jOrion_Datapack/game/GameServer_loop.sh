#!/bin/bash

err=1
until [ $err == 0 ];
do
		[ -f log/error.log.0 ] && mv log/error.log.0 "log/`date +[%Y-%m-%d]_[%H:%M:%S]`_error.log"
        [ -f log/java.log.0 ] && mv log/java.log.0 "log/`date +[%Y-%m-%d]_[%H:%M:%S]`_java.log"
        [ -f log/chat.log ] && mv log/chat.log "log/`date +[%Y-%m-%d]_[%H:%M:%S]`-chat.log"
		[ -f log/item.log ] && mv log/item.log "log/`date +[%Y-%m-%d]_[%H:%M:%S]`-item.log"
		[ -f console.log ] && mv console.log "log/`date +[%Y-%m-%d]_[%H:%M:%S]`_console.log"
		
       java -Djava.util.logging.manager=l2jorion.util.L2LogManager -Xms2g -Xmx2g -cp ../libs/*:l2jorion-core.jar l2jorion.game.GameServer > console.log 2>&1
	   err=$?
        sleep 10
done
