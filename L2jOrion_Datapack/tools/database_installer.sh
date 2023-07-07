#!/bin/bash
trap finish 2

configure() {
echo "#############################################"
echo "# You entered script configuration area     #"
echo "# No change will be performed in your DB    #"
echo "# I will just ask you some questions about  #"
echo "# your hosts and DB.                        #"
echo "#############################################"
MYSQLDUMPPATH=`which -a mysqldump 2>/dev/null`
MYSQLPATH=`which -a mysql 2>/dev/null`
if [ $? -ne 0 ]; then
echo "We were unable to find MySQL binaries on your path"
while :
 do
  echo -ne "\nPlease enter MySQL binaries directory (no trailing slash): "
  read MYSQLBINPATH
    if [ -e "$MYSQLBINPATH" ] && [ -d "$MYSQLBINPATH" ] && \
       [ -e "$MYSQLBINPATH/mysqldump" ] && [ -e "$MYSQLBINPATH/mysql" ]; then
       MYSQLDUMPPATH="$MYSQLBINPATH/mysqldump"
       MYSQLPATH="$MYSQLBINPATH/mysql"
       break
    else
       echo "The data you entered is invalid. Please verify and try again."
       exit 1
    fi
 done
fi
#LS
echo -ne "\nPlease enter MySQL Login Server hostname (default localhost): "
read LSDBHOST
if [ -z "$LSDBHOST" ]; then
  LSDBHOST="localhost"
fi
echo -ne "\nPlease enter MySQL Login Server database name (default serveron): "
read LSDB
if [ -z "$LSDB" ]; then
  LSDB="l2_database"
fi
echo -ne "\nPlease enter MySQL Login Server user (default root): "
read LSUSER
if [ -z "$LSUSER" ]; then
  LSUSER="root"
fi
echo -ne "\nPlease enter MySQL Login Server $LSUSER's password (won't be displayed) :"
stty -echo
read LSPASS
stty echo
echo ""
if [ -z "$LSPASS" ]; then
  echo "Hum.. I'll let it be but don't be stupid and avoid empty passwords"
elif [ "$LSUSER" == "$LSPASS" ]; then
  echo "You're not too brilliant choosing passwords huh?"
fi
#GS
echo -ne "\nPlease enter MySQL Game Server hostname (default $LSDBHOST): "
read GSDBHOST
if [ -z "$GSDBHOST" ]; then
  GSDBHOST="localhost"
fi
echo -ne "\nPlease enter MySQL Game Server database name (default serveron): "
read GSDB
if [ -z "$GSDB" ]; then
  GSDB="l2_database"
fi
echo -ne "\nPlease enter MySQL Game Server user (default $LSUSER): "
read GSUSER
if [ -z "$GSUSER" ]; then
  GSUSER="root"
fi
echo -ne "\nPlease enter MySQL Game Server $GSUSER's password (won't be displayed): "
stty -echo
read GSPASS
stty echo
echo ""
if [ -z "$GSPASS" ]; then
  echo "Hum.. I'll let it be but don't be stupid and avoid empty passwords"
elif [ "$GSUSER" == "$GSPASS" ]; then
  echo "You're not too brilliant choosing passwords huh?"
fi
save_config $1
}

save_config() {
if [ -n "$1" ]; then
CONF="$1"
else 
CONF="database_installer.rc"
fi
echo ""
echo "With these data I can generate a configuration file which can be read"
echo "on future updates. WARNING: this file will contain clear text passwords!"
echo -ne "Shall I generate config file $CONF? (Y/n):"
read SAVE
if [ "$SAVE" == "y" -o "$SAVE" == "Y" -o "$SAVE" == "" ];then 
cat <<EOF>$CONF
#Configuration settings for L2jOrion database installer script
MYSQLDUMPPATH=$MYSQLDUMPPATH
MYSQLPATH=$MYSQLPATH
LSDBHOST=$LSDBHOST
LSDB=$LSDB
LSUSER=$LSUSER
LSPASS=$LSPASS
GSDBHOST=$GSDBHOST
GSDB=$GSDB
GSUSER=$GSUSER
GSPASS=$GSPASS
EOF
chmod 600 $CONF
echo "Configuration saved as $CONF"
echo "Permissions changed to 600 (rw- --- ---)"
elif [ "$SAVE" != "n" -a "$SAVE" != "N" ]; then
  save_config
fi
}

load_config() {
if [ -n "$1" ]; then
CONF="$1"
else 
CONF="database_installer.rc"
fi
if [ -e "$CONF" ] && [ -f "$CONF" ]; then
. $CONF
else
echo "Settings file not found: $CONF"
echo "You can specify an alternate settings filename:"
echo $0 config_filename
echo ""
echo "If file doesn't exist it can be created"
echo "If nothing is specified script will try to work with ./database_installer.rc"
echo ""
configure $CONF
fi
}

asklogin(){
clear
echo "#############################################"
echo "# WARNING: This section of the script CAN   #"
echo "# destroy your characters and accounts      #"
echo "# information. Read questions carefully     #"
echo "# before you reply.                         #"
echo "#############################################"
echo ""
echo "Choose full (f) if you don't have and 'accounts' table or would"
echo "prefer to erase the existing accounts information."
echo "Choose skip (s) to skip loginserver DB installation and go to"
echo "communityserver DB installation/upgrade."
echo -ne "LOGINSERVER DB install type: (f) full, (s) skip or (q) quit? "
read LOGINPROMPT
case "$LOGINPROMPT" in
	"f"|"F") logininstall; loginupgrade; cbbackup; askcbtype;;
	"s"|"S") cbbackup; askcbtype;;
	"q"|"Q") finish;;
	*) asklogin;;
esac
}

logininstall(){
echo "Deleting loginserver tables for new content."
$MYL < ls_cleanup.sql
}

loginupgrade(){
clear
echo "Installling new loginserver content."
for login in $(ls ./sql/login/*.sql);do
	echo "Installing loginserver table : $login"
	$MYL < $login
done
}

gsbackup(){
while :
  do
   echo ""
   echo -ne "Do you want to make a backup copy of your GSDB? (y/n): "
   read LSB
   if [ "$LSB" == "Y" -o "$LSB" == "y" ]; then
     echo "Making a backup of the original gameserver database."
     $MYSQLDUMPPATH --add-drop-table -h $GSDBHOST -u $GSUSER --password=$GSPASS $GSDB > gs_backup.sql
     if [ $? -ne 0 ];then
	 clear
     echo ""
     echo "There was a problem accesing your GS database, either it wasnt created or authentication data is incorrect."
     exit 1
     fi
     break
   elif [ "$LSB" == "n" -o "$LSB" == "N" ]; then 
     break
   fi
  done 
}

lsbackup(){
while :
  do
   clear
   echo ""
   echo -ne "Do you want to make a backup copy of your LSDB? (y/n): "
   read LSB
   if [ "$LSB" == "Y" -o "$LSB" == "y" ]; then
     echo "Making a backup of the original loginserver database."
     $MYSQLDUMPPATH --add-drop-table -h $LSDBHOST -u $LSUSER --password=$LSPASS $LSDB > ls_backup.sql
     if [ $? -ne 0 ];then
        clear
		echo ""
        echo "There was a problem accesing your LS database, either it wasnt created or authentication data is incorrect."
        exit 1
     fi
     break
   elif [ "$LSB" == "n" -o "$LSB" == "N" ]; then 
     break
   fi
  done 
}

asktype(){
echo ""
echo ""
echo "WARNING: A full install (f) will destroy all existing character data."
echo -ne "GAMESERVER DB install type: (f) full install, (u) upgrade, (s) skip or (q) quit? "
read INSTALLTYPE
case "$INSTALLTYPE" in
	"f"|"F") fullinstall; upgradeinstall I; custom;;
	"u"|"U") upgradeinstall U; custom;;
	"s"|"S") custom;;
	"q"|"Q") finish;;
	*) asktype;;
esac
}

askcbtype(){
clear
echo ""
echo ""
echo "WARNING: A full install (f) will destroy all existing community data."
echo -ne "COMMUNITYSERVER DB install type: (f) full install, (u) upgrade, (s) skip or (q) quit? "
read INSTALLTYPE
case "$INSTALLTYPE" in
	"f"|"F") fullcbinstall; upgradecbinstall I; gsbackup; asktype;;
	"u"|"U") upgradecbinstall U; gsbackup; asktype;;
	"s"|"S") gsbackup; asktype;;
	"q"|"Q") finish;;
	*) asktype;;
esac
}

fullcbinstall(){
echo "Deleting all community server tables for new content."
$MYC < cs_cleanup.sql
}

upgradecbinstall(){
clear
if [ "$1" == "I" ]; then 
echo "Installing new community server content."
else
echo "Upgrading community server content"
fi
if [ "$1" == "I" ]; then
	for cb in $(ls ./cb_sql/*.sql);do
		echo "Installing Community Board table : $cb"
		$MYC < $cb
	done
fi
newbie_helper_cb
}

fullinstall(){
clear
echo "Deleting all gameserver tables for new content."
$MYG < gs_cleanup.sql
}

upgradeinstall(){
clear
if [ "$1" == "I" ]; then 
echo "Installing new game server content."
else
echo "Upgrading gameserver content"
fi

for gs in $(ls ./sql/server/*.sql);do
	echo "Installing GameServer table : $gs"
	$MYG < $gs
done

newbie_helper
}

custom(){
echo ""
echo ""
echo -ne "Install custom gameserver DB tables: (y) yes or (n) no or (q) quit?"
read ASKCS
case "$ASKCS" in
	"y"|"Y") cstinstall;;
	"n"|"N") finish;;
	"q"|"Q") finish;;
	*) custom;;
esac
finish
}

cstinstall(){
while :
  do
   clear
   echo ""
   echo -ne "Do you want to make another backup of GSDB before applying custom contents? (y/N): "
   read LSB
   if [ "$LSB" == "Y" -o "$LSB" == "y" ]; then
     echo "Making a backup of the default gameserver tables."
     $MYSQLDUMPPATH --add-drop-table -h $GSDBHOST -u $GSUSER --password=$GSPASS $GSDB > custom_backup.sql 2> /dev/null
     if [ $? -ne 0 ];then
     echo ""
     echo "There was a problem accessing your GS database, server down?."
     exit 1
     fi
     break
   elif [ "$LSB" == "n" -o "$LSB" == "N" -o "$LSB" == "" ]; then 
     break
   fi
  done 
clear
echo "Installing custom content."
for custom in $(ls ./sql/server/custom/*.sql);do 
	echo "Installing custom table: $custom"
	$MYG < $custom
done
# L2J mods that needed extra tables to work properly, should be 
# listed here. To do so copy & paste the following 6 lines and
# change them properly:
# MOD: Wedding.
	echo -ne "Install "Wedding Mod" tables? (y/N): "
	read modprompt
	if [ "$modprompt" == "y" -o "$modprompt" == "Y" ]; then
		for mod in $(ls ./sql/server/mods/*.sql);do
			echo "Installing custom mod table : $mod"
			$MYG < $mod
		done
	fi

finish
}

finish(){
clear
echo "Script execution finished."
echo ""
echo "Thank you for using L2jOrion Server Pack."
echo "visit https://www.l2jorion.com for more info about"
echo "our project."
exit 0
}

newbie_helper(){
while :
  do
   echo ""
   echo -ne "If you're not that skilled applying changes within 'updates' folder, i can try to do it for you (y). If you wish to do it on your own, choose (n). Should i parse updates files? (Y/n)"
   read NOB
   if [ "$NOB" == "Y" -o "$NOB" == "y" -o "$NOB" == "" ]; then
     clear
	 echo ""
     echo "There we go, it may take some time..."
	 echo "Installing Gameserver Updates"
     for file in $(ls ./sql/server/updates/*.sql);do
        $MYG < $file 2>> gserror.log
	 done
	 echo "Installing Loginserver Updates"
	 for file in $(ls ./sql/login/updates/*.sql);do
		$MYL < $file 2>> lserror.log
	 done
     break
   elif [ "$NOB" == "n" -o "$NOB" == "N" ]; then 
     break
   fi
  done 
}

newbie_helper_cb(){
while :
  do
   echo ""
   echo -ne "If you're not that skilled applying changes within 'updates' folder, i can try to do it for you (y). If you wish to do it on your own, choose (n). Should i parse updates files? (Y/n)"
   read NOB
   if [ "$NOB" == "Y" -o "$NOB" == "y" -o "$NOB" == "" ]; then
     clear
	 echo ""
     echo "There we go, it may take some time..."
     echo "updates parser results. Last run: "`date` >cb_database_installer.log
     for file in $(ls ./cb_sql/updates/*sql);do
        echo $file|cut -d/ -f4 >> cb_database_installer.log
        $MYC < $file 2>> cb_database_installer.log
        if [ $? -eq 0 ];then
            echo "no errors">> cb_database_installer.log
        fi
     done
	 clear
     echo ""
     echo "Log available at $(pwd)/cb_database_installer.log"
     echo ""
     break
   elif [ "$NOB" == "n" -o "$NOB" == "N" ]; then
     break
   fi
  done 
}

clear
load_config $1
MYL="$MYSQLPATH -h $LSDBHOST -u $LSUSER --password=$LSPASS -D $LSDB"
MYG="$MYSQLPATH -h $GSDBHOST -u $GSUSER --password=$GSPASS -D $GSDB"
MYC="$MYSQLPATH -h $CBDBHOST -u $CBUSER --password=$CBPASS -D $CBDB"
lsbackup
asklogin