#!/bin/ksh
#
#su -  c /home/OMSAutomationScripts/dtc_orders_stuck_created_await_payment_info.sh

ORACLE_HOME=/u01/app/oracle/product/11.2.0/db
LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
TNS_ADMIN=$ORACLE_HOME/network/admin
PATH=$ORACLE_HOME/bin:$PATH:.


#RUN_SCRIPT (Value should be QA or PROD)

RUN_SCRIPT=PROD


if [ $RUN_SCRIPT == "PROD" ] 
then    	
	USER=sterling_reader01
	PWD=xpruhjJ3Px
	DatabaseName=OMS_PROD
	HOST_NAME=lin-vsi-omspdb3
	PORT=1881
	SERVICE_SID="(SERVICE_NAME=OMSPROD)"	
	#st3rling0ms, xpruhjJ3Px, lin-vsi-omspdb3, 1881, OMSPROD
else

	ORACLE_HOME=/u01/app/oracle/product/11.2.0/
	LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
	TNS_ADMIN=$ORACLE_HOME/network/admin
	PATH=$ORACLE_HOME/bin:$PATH:.

    USER=sterling
	PWD=st34rl1ng0ms
	DatabaseName=omstest
	HOST_NAME=10.3.53.74
	PORT=1581
	SERVICE_SID="(SID=omstest)"	
fi

#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"



export PATH LD_LIBRARY_PATH TNS_ADMIN USER PWD DB ORACLE_HOME

LOGDIR=/home/OMSAutomationScripts

ARCDIR=$LOGDIR/Arch
LOGFILE4=$LOGDIR/DTC_MonitorOrderResults_AwaitPayInfo.txt

#LOGFILE2=$LOGDIR/MonitorOrderResults2.txt

TO_ADDRS_ON_FAILURE="oms@vitaminshoppe.com"
#TO_ADDRS_ON_FAILURE = "omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com operators@vitaminshoppe.com ooyediran@vitaminshoppe.com UnixAdmins@vitaminshoppe.com"
AZ_ADDRS="oms@vitaminshoppe.com"
AZ_ADDRS="oms@vitaminshoppe.com"

export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD TO_ADDRS_ON_FAILURE

orderCount4=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
SELECT COUNT(DISTINCT(YOH.ORDER_NO)) AS C
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL, STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS
WHERE YOH.ORDER_HEADER_KEY = YORS.ORDER_HEADER_KEY AND YORS.STATUS = YS.STATUS
AND YORS.STATUS_QUANTITY > '0' AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE -120,'YYYYMMDD') AND YOH.DOCUMENT_TYPE = '0001' AND YOH.ORDER_NO NOT LIKE 'Y1%' AND YOH.EXTN_DTC_ORDER = 'Y' AND YOH.DRAFT_ORDER_FLAG = 'N'
AND YORS.STATUS_QUANTITY > '0' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YORS.STATUS < '1500' AND YORS.STATUS NOT IN ('1300','1310','1400')
AND YOH.PAYMENT_STATUS = 'AWAIT_PAY_INFO'
AND SYSDATE - YORS.STATUS_DATE > '0.010';
exit 0
END
)


sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 50000
set linesize 1000
set trimspool on
set feedback off
set heading on
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
spool $LOGFILE4
ALTER SESSION SET nls_date_format = 'DD-MON-RR HH24:MI:SS'; 
SELECT DISTINCT(TRIM(YOH.ORDER_NO)) AS ORDER_NO, TRIM(YOH.ORDER_DATE) AS ORDER_DATE, TRIM(YS.DESCRIPTION) AS STATUS, TRIM(ROUND(SYSDATE - YORS.STATUS_DATE,3)*24*60) AS AGED_MINS, YOH.PAYMENT_STATUS
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS
WHERE YOH.ORDER_HEADER_KEY = YORS.ORDER_HEADER_KEY AND YORS.STATUS = YS.STATUS
AND YORS.STATUS_QUANTITY > '0' AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE -120,'YYYYMMDD') AND YOH.DOCUMENT_TYPE = '0001' AND YOH.ORDER_NO NOT LIKE 'Y1%' AND YOH.EXTN_DTC_ORDER = 'Y' AND YOH.DRAFT_ORDER_FLAG = 'N'
AND YORS.STATUS_QUANTITY > '0' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YORS.STATUS < '1500' AND YORS.STATUS NOT IN ('1300','1310','1400')
AND YOH.PAYMENT_STATUS = 'AWAIT_PAY_INFO'
AND SYSDATE - YORS.STATUS_DATE > '0.010'
ORDER BY TRIM(YOH.ORDER_DATE) ASC;
spool off
exit 0
END

order_msg4=$(cat $LOGFILE4)

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running OMS orders stuck in Created status script" $TO_ADDRS_ON_FAILURE
echo -e "Operators,\n\nPlease check connectivity to OMS servers by logging to App Console 1 and 2. If not able to connect call DB ADMIN.\nDuring weekend and off-hours, also call OMS team (follow OMS escalation).\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE
elif [ $orderCount4 -gt 0 ]

then
echo -e "DTC Orders from OMS PROD stuck in Created status with Awaiting Payment Info.\n\nPlease follow OMS escalation chart if there are more than 5 orders below during WEEKENDS, HOLIDAYS or AFTER OFFICE HOURS.\n\nScript Name:Orders_Stuck_Created_Status.sh \nCriteria: Orders older than 80 mins and upto 120 days.\n\n$order_msg4"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "DTC Orders stuck in Created status with Awaiting Payment Info. Count:$orderCount4" $AZ_ADDRS
fi