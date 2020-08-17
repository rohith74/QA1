#!/bin/ksh
#
#su - shivraj.ugale -c /home/OMSAutomationScripts/Missing_Credit_Card_Expiry_Date.sh

ORACLE_HOME=/u01/app/oracle/product/11.2.0/db
LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
TNS_ADMIN=$ORACLE_HOME/network/admin
PATH=$ORACLE_HOME/bin:$PATH:.

#Change user, pswd and db to point to target database
USER=sterling_reader01
PWD=xpruhjJ3Px
DatabaseName=OMS_PROD

#st3rling0ms, xpruhjJ3Px, 10.3.51.167, 1881, OMSPROD_SVC

export PATH LD_LIBRARY_PATH TNS_ADMIN USER PWD DB ORACLE_HOME

LOGDIR=/home/OMSAutomationScripts

#DATASRC=/opt/web20/open/weber_order_status/production
#SHELLDIR=/export/home/vsadmin/scripts/weber_order_status/production
#SCRIPTNAME=`basename $0`

ARCDIR=$LOGDIR/Arch
LOGFILE1=$LOGDIR/DiscountReport.txt
ZIPFILE1=$LOGDIR/DiscountReport.zip
#LOGFILE2=$LOGDIR/MonitorOrderResults2.txt
TO_ADDRS="oms@vitaminshoppe.com Mirza.Azad@vitaminshoppe.com ccruz@vitaminshoppe.com bskokandich@vitaminshoppe.com"
TO_ADDRS_ON_FAILURE="oms@vitaminshoppe.com Mirza.Azad@vitaminshoppe.com ccruz@vitaminshoppe.com bskokandich@vitaminshoppe.com"
#TO_ADDRS_ON_FAILURE="shivraj.ugale@vitaminshoppe.com"
#TO_ADDRS="shivraj.ugale@vitaminshoppe.com"
export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD TO_ADDRS TO_ADDRS_ON_FAILURE ZIPFILE1

#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"

sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set linesize 3000
set trimspool on
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
spool $LOGFILE1
ALTER SESSION SET nls_date_format = 'DD-MON-RR HH24:MI:SS';
SELECT * FROM 
(
SELECT TRIM(CASE
WHEN YOH.EXTN_ORIGINAL_ADP_ORDER IN ('Y') THEN 'ADP INITIAL'
WHEN YOH.EXTN_SUBSCRIPTION_ORDER IN ('Y') THEN 'ADP REPLENISHMENT'
WHEN YOH.ENTRY_TYPE IN ('Marketplace') THEN 'MARKETPLACE'
WHEN YOH.ENTRY_TYPE = 'WEB' AND YOL.LINE_TYPE IN ('PICK_IN_STORE','SHIP_TO_STORE') THEN 'BOPS'
WHEN YOH.ENTRY_TYPE = 'WEB' AND YOL.LINE_TYPE IN ('SHIP_TO_HOME') THEN 'WEB'
WHEN YOH.ENTRY_TYPE = 'STORE' THEN 'SPECIAL ORDER' 
WHEN YOH.ENTRY_TYPE = 'Call Center' THEN 'CALL CENTER' 
ELSE 'NA_CATEGORY' END) ||'|'||
TRIM(TRIM(YLC.CREATEUSERID)||'|'||YOH.ORDER_NO||'|'||TRIM((CUSTOMER_FIRST_NAME ||' '|| CUSTOMER_LAST_NAME))||'|'||TRIM(YOH.BILL_TO_ID)||'|'||
TRIM(SUBSTR(YLC.CREATETS,1,9))||'|'||TRIM(SUBSTR(YLC.CREATETS,11,8))||'|-'||TRIM(YLC.CHARGEAMOUNT)||'|'||TRIM(YLC.CHARGE_CATEGORY)||'|'||TRIM(YLC.CHARGE_NAME)||'|'||
TRIM(YOH.EXTN_PRICING_DATE)) AS DISCOUNT_REPORT
FROM STERLING.YFS_LINE_CHARGES YLC, STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL
WHERE YLC.LINE_KEY = YOL.ORDER_LINE_KEY AND YOH.ORDER_HEADER_KEY = YOL.ORDER_HEADER_KEY 
AND YLC.LINE_KEY > '20180101'
AND YLC.CHARGEAMOUNT <> '0'
AND YLC.CREATEUSERID IN (SELECT TRIM(LOGINID) FROM STERLING.YFS_USER WHERE DATA_SECURITY_GROUP_ID in ('CSR-TEAM','STORE-TEAM'))
AND YOH.ORDER_HEADER_KEY NOT IN (SELECT YOI.ORDER_HEADER_KEY FROM STERLING.YFS_ORDER_INVOICE YOI)
ORDER BY YLC.CREATETS ASC
) ADJ_PRE_SALE_NOT_INVOICED
UNION ALL
SELECT * FROM
(
SELECT TRIM(CASE
WHEN YOH.EXTN_ORIGINAL_ADP_ORDER IN ('Y') THEN 'ADP INITIAL'
WHEN YOH.EXTN_SUBSCRIPTION_ORDER IN ('Y') THEN 'ADP REPLENISHMENT'
WHEN YOH.ENTRY_TYPE IN ('Marketplace') THEN 'MARKETPLACE'
WHEN YOH.ENTRY_TYPE = 'WEB' AND YOL.LINE_TYPE IN ('PICK_IN_STORE','SHIP_TO_STORE') THEN 'BOPS'
WHEN YOH.ENTRY_TYPE = 'WEB' AND YOL.LINE_TYPE IN ('SHIP_TO_HOME') THEN 'WEB'
WHEN YOH.ENTRY_TYPE = 'STORE' THEN 'SPECIAL ORDER' 
WHEN YOH.ENTRY_TYPE = 'Call Center' THEN 'CALL CENTER' 
ELSE 'NA_CATEGORY' END) ||'|'||
TRIM(TRIM(YLC.CREATEUSERID)||'|'||YOH.ORDER_NO||'|'||TRIM((CUSTOMER_FIRST_NAME ||' '|| CUSTOMER_LAST_NAME))||'|'||TRIM(YOH.BILL_TO_ID)||'|'||TRIM(SUBSTR(YLC.CREATETS,1,9))||'|'||
TRIM(SUBSTR(YLC.CREATETS,11,8))||'|-'||TRIM(YLC.CHARGEAMOUNT)||'|'||TRIM(YLC.CHARGE_CATEGORY)||'|'||TRIM(YLC.CHARGE_NAME)||'|'||TRIM(YOH.EXTN_PRICING_DATE)) AS DISCOUNT_REPORT
FROM STERLING.YFS_LINE_CHARGES YLC, STERLING.YFS_ORDER_INVOICE YOI, STERLING.YFS_ORDER_INVOICE_DETAIL YOID, STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL
WHERE YOI.ORDER_INVOICE_KEY = YLC.HEADER_KEY AND YLC.LINE_KEY = YOID.ORDER_INVOICE_DETAIL_KEY AND YOI.ORDER_HEADER_KEY = YOH.ORDER_HEADER_KEY AND YOID.ORDER_LINE_KEY = YOL.ORDER_LINE_KEY
AND YOI.ORDER_INVOICE_KEY > '20180101'
AND YOI.INVOICE_TYPE NOT IN ('CREDIT_MEMO','INFO','RETURN','DEBIT_MEMO')
AND YLC.CREATEUSERID IN (SELECT TRIM(LOGINID) FROM STERLING.YFS_USER WHERE DATA_SECURITY_GROUP_ID in ('CSR-TEAM','STORE-TEAM'))
ORDER BY YLC.CREATETS ASC
) ADJ_POST_SALE_SHIPMENT_INV
UNION ALL
SELECT * FROM
(
SELECT TRIM(CASE
WHEN YOH.EXTN_ORIGINAL_ADP_ORDER IN ('Y') THEN 'ADP INITIAL'
WHEN YOH.EXTN_SUBSCRIPTION_ORDER IN ('Y') THEN 'ADP REPLENISHMENT'
WHEN YOH.ENTRY_TYPE IN ('Marketplace') THEN 'MARKETPLACE'
WHEN YOH.ENTRY_TYPE = 'WEB' AND YOL.LINE_TYPE IN ('PICK_IN_STORE','SHIP_TO_STORE') THEN 'BOPS'
WHEN YOH.ENTRY_TYPE = 'WEB' AND YOL.LINE_TYPE IN ('SHIP_TO_HOME') THEN 'WEB'
WHEN YOH.ENTRY_TYPE = 'STORE' THEN 'SPECIAL ORDER' 
WHEN YOH.ENTRY_TYPE = 'Call Center' THEN 'CALL CENTER' 
ELSE 'NA_CATEGORY' END) ||'|'||
TRIM(TRIM(YLC.CREATEUSERID)||'|'||YOH.ORDER_NO||'|'||TRIM((CUSTOMER_FIRST_NAME ||' '|| CUSTOMER_LAST_NAME))||'|'||TRIM(YOH.BILL_TO_ID)||'|'||TRIM(SUBSTR(YLC.CREATETS,1,9))||'|'||
TRIM(SUBSTR(YLC.CREATETS,11,8))||'|-'||TRIM(YLC.CHARGEAMOUNT)||'|'||TRIM(YLC.CHARGE_CATEGORY)||'|'||TRIM(YLC.CHARGE_NAME)) AS DISCOUNT_REPORT
FROM STERLING.YFS_LINE_CHARGES YLC, STERLING.YFS_ORDER_INVOICE YOI, STERLING.YFS_ORDER_INVOICE_DETAIL YOID, STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL
WHERE YOI.ORDER_INVOICE_KEY = YLC.HEADER_KEY AND YLC.LINE_KEY = YOID.ORDER_INVOICE_DETAIL_KEY AND YOI.ORDER_HEADER_KEY = YOH.ORDER_HEADER_KEY AND YOID.ORDER_LINE_KEY = YOL.ORDER_LINE_KEY
AND YOI.ORDER_INVOICE_KEY > '20180101'
AND YOI.INVOICE_TYPE IN ('CREDIT_MEMO')
ORDER BY YLC.CREATETS ASC
) ADJ_POST_SALE_CREDIT_MEMO_INV
UNION ALL
SELECT * FROM
(
SELECT TRIM(CASE
WHEN YOH.EXTN_ORIGINAL_ADP_ORDER IN ('Y') THEN 'ADP INITIAL'
WHEN YOH.EXTN_SUBSCRIPTION_ORDER IN ('Y') THEN 'ADP REPLENISHMENT'
WHEN YOH.ENTRY_TYPE IN ('Marketplace') THEN 'MARKETPLACE'
WHEN YOH.ENTRY_TYPE = 'WEB' THEN 'WEB'
WHEN YOH.ENTRY_TYPE = 'STORE' THEN 'SPECIAL ORDER' 
WHEN YOH.ENTRY_TYPE = 'Call Center' THEN 'CALL CENTER' 
ELSE 'NA_CATEGORY' END) ||'|'||
TRIM(TRIM(YOI.CREATEUSERID)||'|'||YOH.ORDER_NO||'|'||TRIM(CUSTOMER_FIRST_NAME ||' '|| CUSTOMER_LAST_NAME)||'|'||TRIM(YOH.BILL_TO_ID)||'|'||TRIM(SUBSTR(YOI.CREATETS,1,9))||'|'||
TRIM(SUBSTR(YOI.CREATETS,11,8))||'|-'||TRIM(YOI.EXTN_APPEASE_AMOUNT)||'|'||'CUSTOMER_APPEASEMENT'||'|'||TRIM(YOI.INVOICE_TYPE)) AS APPEASEMENT_REPORT
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_INVOICE YOI
WHERE YOH.ORDER_HEADER_KEY=YOI.ORDER_HEADER_KEY
AND YOI.ORDER_INVOICE_KEY > '20180101'
AND YOI.INVOICE_TYPE IN ('INFO')
AND YOI.CREATEUSERID IN (SELECT TRIM(LOGINID) FROM STERLING.YFS_USER WHERE DATA_SECURITY_GROUP_ID in ('CSR-TEAM','STORE-TEAM'))
ORDER BY YOI.CREATETS ASC
) INFO_INVOICE
UNION ALL
SELECT * FROM
(
SELECT TRIM(CASE
WHEN YOH.EXTN_ORIGINAL_ADP_ORDER IN ('Y') THEN 'ADP INITIAL'
WHEN YOH.EXTN_SUBSCRIPTION_ORDER IN ('Y') THEN 'ADP REPLENISHMENT'
WHEN YOH.ENTRY_TYPE IN ('Marketplace') THEN 'MARKETPLACE'
WHEN YOH.ENTRY_TYPE = 'WEB' AND YOL.LINE_TYPE IN ('PICK_IN_STORE','SHIP_TO_STORE') THEN 'BOPS'
WHEN YOH.ENTRY_TYPE = 'WEB' AND YOL.LINE_TYPE IN ('SHIP_TO_HOME') THEN 'WEB'
WHEN YOH.ENTRY_TYPE = 'STORE' THEN 'SPECIAL ORDER' 
WHEN YOH.ENTRY_TYPE = 'Call Center' THEN 'CALL CENTER' 
ELSE 'NA_CATEGORY' END) ||'|'||
TRIM(TRIM(YOH.CREATEUSERID)||'|'||YOH.ORDER_NO||'|'||TRIM(YOH.CUSTOMER_FIRST_NAME ||' '|| YOH.CUSTOMER_LAST_NAME)||'|'||TRIM(YOH.BILL_TO_ID)||'|'||TRIM(SUBSTR(YOH.CREATETS,1,9))||'|'||
TRIM(SUBSTR(YOH.CREATETS,11,8))||'|'||TRIM(YOL.UNIT_PRICE - YOL.LIST_PRICE)||'|PRICE_CHANGE'||'|'||'PRICE MODIFIED BY CSR') AS PRICE_CHANGE
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL
WHERE YOH.ORDER_HEADER_KEY = YOL.ORDER_HEADER_KEY 
AND YOH.DOCUMENT_TYPE = '0001' 
AND YOH.DRAFT_ORDER_FLAG = 'N'
AND YOH.ENTRY_TYPE = 'Call Center'
AND YOH.ORDER_HEADER_KEY > '20180101'
AND YOL.UNIT_PRICE <> YOL.LIST_PRICE
AND YOH.CREATEUSERID IN (SELECT TRIM(LOGINID) FROM STERLING.YFS_USER WHERE DATA_SECURITY_GROUP_ID in ('CSR-TEAM','STORE-TEAM'))
ORDER BY YOH.CREATETS ASC
) PRICE_CHANGE
;
spool off
exit 0
END

order_msg=$(cat $LOGFILE1)

zip DiscountReport.zip DiscountReport.txt

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running CSR Discount and Appeasement report script" $TO_ADDRS_ON_FAILURE
echo -e "Error running CSR Discount and Appeasement report script. Please contact OMS Support.\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE
else
REPORTDATE=`date +%Y_%m_%d`
echo -e "Hello,\n\nPFA CSR Discount and Appeasement report.\n\nThanks"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "CSR Discount & Appeasement Report $REPORTDATE" -a $LOGDIR/DiscountReport.zip $TO_ADDRS
fi