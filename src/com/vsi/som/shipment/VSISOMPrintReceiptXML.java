package com.vsi.som.shipment;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFApi;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSISOMPrintReceiptXML {
	
	private static YFCLogCategory log = YFCLogCategory.instance(VSISOMPrintReceiptXML.class);
	private static final String TAG = VSISOMPrintReceiptXML.class.getSimpleName();
	YIFApi api;
	
	public Document printPickPackReceipt(YFSEnvironment env, Document inXML){
		
		printLogs("================Inside VSISOMPrintReceiptXML Class and printPickPackReceipt Method================");
		printLogs("Printing Input XML :"+SCXmlUtil.getString(inXML));
		
		try{
			
			Element eleShipment=inXML.getDocumentElement();			
			String strPickPackFlag=null;			
			String strShipmentKey =eleShipment.getAttribute(VSIConstants.ATTR_SHIPMENT_KEY);			
			Document docGetShipmentListIn=XMLUtil.createDocument(VSIConstants.ELE_SHIPMENT);
			Element eleGetShipmentList = docGetShipmentListIn.getDocumentElement();
			eleGetShipmentList.setAttribute(VSIConstants.ATTR_SHIPMENT_KEY, strShipmentKey);	
			
			Document docGetShipmentListOut = VSIUtils.invokeAPI(env,VSIConstants.SHIPMENT_LIST_PICK_PACK_TEMPLATE,VSIConstants.API_GET_SHIPMENT_LIST, docGetShipmentListIn);
			Element eleGetShipmentListOut = (Element) docGetShipmentListOut.getElementsByTagName(VSIConstants.ELE_SHIPMENTS).item(0);			
			
			int intTotalNoOfShipment= Integer.parseInt(eleGetShipmentListOut.getAttribute(VSIConstants.ATTR_TOTAL_NUMBER_OF_RECORDS));
			Element eleShipmentOut=null;
			Element eleShpmntExtn= null;
			String strShipmentStatus = null;
			//NewChanges-14Aug2020- START	
			Element eleShipmntLines=null;
			Element eleShipmntLine=null;
			Element eleOrdrLine= null;
			String lineType= null;
			String delMethod=null;
			//NewChanges-14Aug2020- END	
			if (intTotalNoOfShipment > 0) {
				 eleShipmentOut= (Element) docGetShipmentListOut.getElementsByTagName(VSIConstants.ELE_SHIPMENT).item(0);
				 eleShpmntExtn=SCXmlUtil.getChildElement(eleShipmentOut, VSIConstants.ELE_EXTN);
				//NewChanges-14Aug2020- START
				 eleShipmntLines=SCXmlUtil.getChildElement(eleShipmentOut, VSIConstants.ELE_SHIPMENT_LINES);
				 eleShipmntLine=SCXmlUtil.getChildElement(eleShipmntLines, VSIConstants.ELE_SHIPMENT_LINE);
				 eleOrdrLine= SCXmlUtil.getChildElement(eleShipmntLine, VSIConstants.ELE_ORDER_LINE);
				 lineType= eleOrdrLine.getAttribute(VSIConstants.ATTR_LINE_TYPE);
				 delMethod= eleOrdrLine.getAttribute(VSIConstants.ATTR_DELIVERY_METHOD);
				//NewChanges-14Aug2020- END	
				 if(!YFCCommon.isVoid(eleShpmntExtn)){
					 strPickPackFlag=eleShpmntExtn.getAttribute("ExtnPickPackFlag");
				 }
				 strShipmentStatus= eleShipmentOut.getAttribute(VSIConstants.ATTR_STATUS);
			}
			
	
		if(VSIConstants.FLAG_Y.equals(strPickPackFlag)){
				printLogs("PickPackReceipt Process has already been completed for this Shipment");
				printLogs("Exiting VSISOMPrintReceiptXML Class and printPickPackReceipt Method");
				return inXML;
		}else{
			//NewChanges-14Aug2020- START
			if((strShipmentStatus.equalsIgnoreCase("1100.70.06.30") || strShipmentStatus.equalsIgnoreCase("1100.70.06.50")) && ( (delMethod.equalsIgnoreCase("PICK")) || (lineType.equalsIgnoreCase("PICK_IN_STORE")))) {
			//NewChanges-14Aug2020- END	
				Document docPickPackXML=XMLUtil.createDocument("OrderData");
				Element elePickPackXML=docPickPackXML.getDocumentElement();
				Document docSortedShpmntIn = XMLUtil.createDocument(VSIConstants.ELE_SHIPMENT);
				Element eleSortedShpmntIn = docSortedShpmntIn.getDocumentElement();
				eleSortedShpmntIn.setAttribute(VSIConstants.ATTR_SHIPMENT_KEY, strShipmentKey);
				
				printLogs("Input to getSortedShipmentDetails API: "+SCXmlUtil.getString(docSortedShpmntIn));
				log.info("Input to getSortedShipmentDetails API: "+SCXmlUtil.getString(docSortedShpmntIn));
				Document docSortedShpmntOut = VSIUtils.invokeAPI(env,"global/template/api/VSISOMCustomerShipmentSorted.xml","getSortedShipmentDetails", docSortedShpmntIn);
				printLogs("Output from getSortedShipmentDetails API: "+SCXmlUtil.getString(docSortedShpmntOut));
				log.info("Output from getSortedShipmentDetails API: "+SCXmlUtil.getString(docSortedShpmntOut));
				
				Element eleSortedShpmntOut=docSortedShpmntOut.getDocumentElement();
				String strStoreNo=eleSortedShpmntOut.getAttribute(VSIConstants.ATTR_SHIP_NODE);
				String strStoreNumber=("0000" + strStoreNo).substring(strStoreNo.length());			
				String strOrderNumber="";
				String strCustPONo=eleSortedShpmntOut.getAttribute(VSIConstants.ATTR_SHIP_CUST_PO_NO);
				if(!YFCCommon.isVoid(strCustPONo)){
					strOrderNumber=strCustPONo;
				}else{
					strOrderNumber=eleSortedShpmntOut.getAttribute(VSIConstants.ATTR_ORDER_NO);
				}
				Element eleFromAddr = SCXmlUtil.getChildElement(eleSortedShpmntOut, "FromAddress");
				String strStoreAddressLine1=eleFromAddr.getAttribute(VSIConstants.ATTR_ADDRESS1);
				String strStoreAddressLine2=eleFromAddr.getAttribute(VSIConstants.ATTR_ADDRESS2);
				String strStoreCity=eleFromAddr.getAttribute(VSIConstants.ATTR_CITY);
				String strStoreState=eleFromAddr.getAttribute(VSIConstants.ATTR_STATE);
				String strStoreZipCode=eleFromAddr.getAttribute(VSIConstants.ATTR_ZIPCODE);
				String strStorePhoneNumber=eleFromAddr.getAttribute(VSIConstants.ATTR_DAY_PHONE);
				
				Element eleShipmentLines=SCXmlUtil.getChildElement(eleSortedShpmntOut, VSIConstants.ELE_SHIPMENT_LINES);				
				Element eleShipmentLine=SCXmlUtil.getChildElement(eleShipmentLines, VSIConstants.ELE_SHIPMENT_LINE);				
				Element eleOrder=SCXmlUtil.getChildElement(eleShipmentLine, VSIConstants.ELE_ORDER);
				
				String strCustomerName=eleOrder.getAttribute(VSIConstants.ATTR_CUSTOMER_FIRST_NAME);
				String strCustomerNumber=eleOrder.getAttribute(VSIConstants.ATTR_BILL_TO_ID);
				
				//NewChanges-14Aug2020- START		
				Element eleBillTo = SCXmlUtil.getChildElement(eleSortedShpmntOut, VSIConstants.ELE_BILL_TO_ADDRESS);
				String strCustomerEmail=eleBillTo.getAttribute(VSIConstants.ATTR_EMAIL_ID);
				String strCustomerAdd1=eleBillTo.getAttribute(VSIConstants.ATTR_ADDRESS1);
				String strCustomerAdd2=eleBillTo.getAttribute(VSIConstants.ATTR_ADDRESS2);
				String strCustomerCity=eleBillTo.getAttribute(VSIConstants.ATTR_CITY);
				String strCustomerState=eleBillTo.getAttribute(VSIConstants.ATTR_STATE);
				String strCustomerZipCode=eleBillTo.getAttribute(VSIConstants.ATTR_ZIPCODE);
				String strCustomerPhoneNumber=eleBillTo.getAttribute(VSIConstants.ATTR_DAY_PHONE);
				//NewChanges-14Aug2020- END
				
				String strEntryType=eleOrder.getAttribute(VSIConstants.ATTR_ENTRY_TYPE);			
				Element elePaymentMethods=SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_PAYMENT_METHODS);
				Element eleOrderExtn=SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_EXTN);
				String strMembershipStatus=eleOrderExtn.getAttribute(VSIConstants.ATTR_CUSTOMER_TIER);
				String strPointsEarned=eleOrderExtn.getAttribute("ExtnPointsEarned");
				String strTotalPointsEarned=eleOrderExtn.getAttribute("ExtnTotalPointsEarned");
				String strOrderPoints="";
				String strCurrentBalance="";
				if(VSIConstants.WEB.equals(strEntryType)){
					if(!YFCCommon.isVoid(strPointsEarned)){
						strOrderPoints=strPointsEarned;
					}
					if(!YFCCommon.isVoid(strTotalPointsEarned)){
						strCurrentBalance=strTotalPointsEarned;
					}
				}else if(VSIConstants.ENTRYTYPE_CC.equals(strEntryType)){
					if(!YFCCommon.isVoid(strTotalPointsEarned)){
						strOrderPoints=strTotalPointsEarned;
					}
					if(!YFCCommon.isVoid(strPointsEarned)){
						strCurrentBalance=strPointsEarned;
					}
				}
				Element eleOrderLine=SCXmlUtil.getChildElement(eleShipmentLine, VSIConstants.ELE_ORDER_LINE);
				Element eleOrdLnTaxes=SCXmlUtil.getChildElement(eleOrderLine, VSIConstants.ELE_LINE_TAXES);
				Element eleOrdLnTax=SCXmlUtil.getChildElement(eleOrdLnTaxes, VSIConstants.ELE_LINE_TAX);				
				String strTaxPercentage ="";				
				
				if(eleOrdLnTax != null){

					 strTaxPercentage=eleOrdLnTax.getAttribute("TaxPercentage");
				}		
				
				String strDocType = getCommonCodeValues(env,"SOM_DOCUMENT_TYPES","PickPack");
				
				String strFooterMessage = getCommonCodeValues(env,"SOM_FOOTER_MESSAGE","FOOTERMESSAGE");
				
				putElementValue(elePickPackXML,"StoreNumber", strStoreNumber);
				putElementValue(elePickPackXML,"StoreAddressLine1", strStoreAddressLine1);
				putElementValue(elePickPackXML,"StoreAddressLine2", strStoreAddressLine2);
				putElementValue(elePickPackXML,"StoreCity", strStoreCity);
				putElementValue(elePickPackXML,"StoreState", strStoreState);
				putElementValue(elePickPackXML,"StoreZipCode", strStoreZipCode);
				putElementValue(elePickPackXML,"StorePhoneNumber", strStorePhoneNumber);
				putElementValue(elePickPackXML,"DocumentType", strDocType);
				//NewChanges-14Aug2020- START
				putElementValue(elePickPackXML,"CustomerName", strCustomerName);
				putElementValue(elePickPackXML,"CustomerEmail", strCustomerEmail);
				putElementValue(elePickPackXML,"CustomerAddressLine1", strCustomerAdd1);
				putElementValue(elePickPackXML,"CustomerAddressLine2", strCustomerAdd2);
				putElementValue(elePickPackXML,"CustomerCity", strCustomerCity);
				putElementValue(elePickPackXML,"CustomerState", strCustomerState);
				putElementValue(elePickPackXML,"CustomerZipCode", strCustomerZipCode);			
				putElementValue(elePickPackXML,"CustomerNumber", strCustomerNumber);
				putElementValue(elePickPackXML,"CustomerPhoneNumber", strCustomerPhoneNumber);
				//NewChanges-14Aug2020- END
				putElementValue(elePickPackXML,"OrderNumber", strOrderNumber);
				
				Element eleShipNode = SCXmlUtil.getChildElement(eleSortedShpmntOut, VSIConstants.ELE_SHIP_NODE);
				String strLocalecode=eleShipNode.getAttribute("Localecode");
				String strTimeZone="";
				if(VSIConstants.ATTR_EN_US_EST.equals(strLocalecode)){
					strTimeZone="US/Eastern";
				}else if("en_US_CST".equals(strLocalecode)){
					strTimeZone="US/Central";
				}else if("en_US_PST".equals(strLocalecode)){
					strTimeZone="US/Pacific";
				}else if("en_US_HAST".equals(strLocalecode)){
					strTimeZone="US/Hawaii";
				}else if("en_US_MST".equals(strLocalecode)){
					strTimeZone="US/Mountain";
				}else if("en_US_MDT".equals(strLocalecode)){
					strTimeZone="US/Mountain";
				}
				
				Calendar calendar = Calendar.getInstance(); 
				
				SimpleDateFormat sysDt = new SimpleDateFormat("dd MMM yyyy");
		        sysDt.setTimeZone(TimeZone.getTimeZone(strTimeZone));
		        String sysDate = sysDt.format(calendar.getTime());
		        
		        SimpleDateFormat sysTm = new SimpleDateFormat("HH:mm:ss z");
		        sysTm.setTimeZone(TimeZone.getTimeZone(strTimeZone));
		        String sysTime = sysTm.format(calendar.getTime());
		        
		        Element elePayments=SCXmlUtil.createChild(elePickPackXML, "Payments");
				NodeList nlPaymentMethod=elePaymentMethods.getElementsByTagName(VSIConstants.ELE_PAYMENT_METHOD);
				for(int k=0; k<nlPaymentMethod.getLength(); k++){
					Element elePaymentMethod=(Element)nlPaymentMethod.item(k);
					Element elePayment=SCXmlUtil.createChild(elePayments, "Payment");
					String strPaymentType=elePaymentMethod.getAttribute(VSIConstants.ATTR_PAYMENT_TYPE);
					String strPaymentMethod="";
					String strPaymentDetail="";
					String strPaymentAmount="";
					if(VSIConstants.PAYMENT_MODE_CC.equals(strPaymentType)){
						strPaymentMethod=elePaymentMethod.getAttribute(VSIConstants.ATTR_CREDIT_CARD_TYPE);
						strPaymentDetail=elePaymentMethod.getAttribute("CreditCardNo");
						//NewChanges-14Aug2020- START
						//strPaymentAmount=elePaymentMethod.getAttribute("RequestedChargeAmount");
						//strPaymentDetail=strPaymentDetail.substring(strPaymentDetail.length()-4);
						//NewChanges-14Aug2020- END
					}else if(VSIConstants.PAYMENT_MODE_GC.equals(strPaymentType)){
						strPaymentMethod="Gift Card";
						strPaymentDetail=elePaymentMethod.getAttribute("DisplaySvcNo");
					}else if(VSIConstants.PAYMENT_MODE_VOUCHERS.equals(strPaymentType)){
						strPaymentMethod="Health Awards";
						strPaymentDetail=elePaymentMethod.getAttribute(VSIConstants.ATTR_PAYMENT_REFERENCE_1);
					}else if(VSIConstants.PAYMENT_MODE_PP.equals(strPaymentType)){
						strPaymentMethod="PayPal";
						strPaymentDetail=elePaymentMethod.getAttribute(VSIConstants.ATTR_PAYMENT_REFERENCE_3);
					}
					putElementValue(elePayment,"PaymentMethod", strPaymentMethod);
					putElementValue(elePayment,"PaymentDetail", strPaymentDetail);
					//NewChanges-14Aug2020- START
					putElementValue(elePayment,"PaymentAmount", strPaymentAmount);
					//NewChanges-14Aug2020- END
				}
				
				putElementValue(elePickPackXML,"MembershipStatus", strMembershipStatus);
				putElementValue(elePickPackXML,"OrderPoints", strOrderPoints);
				putElementValue(elePickPackXML,"CurrentBalance", strCurrentBalance);
				putElementValue(elePickPackXML,"TranNumber", "");
				putElementValue(elePickPackXML,"Date", sysDate);
				putElementValue(elePickPackXML,"Time", sysTime);
				//putElementValue(elePickPackXML,"Associate", "");
				putElementValue(elePickPackXML,"FooterMessage", strFooterMessage);
							
				
				Element eleOrderLinesOut=SCXmlUtil.createChild(elePickPackXML, VSIConstants.ELE_ORDER_LINES);
				NodeList nlShipmentLine=eleShipmentLines.getElementsByTagName(VSIConstants.ELE_SHIPMENT_LINE);
				double dTotalChargeAmount=0.0;
				double dSubTotal=0.0;
				double dSalesTaxAmnt=0.0;
				double dTotal=0.0;
				for(int i=0; i<nlShipmentLine.getLength(); i++){
					Element eleShpmntLn = (Element) nlShipmentLine.item(i);
					Element eleOrderLineOut=SCXmlUtil.createChild(eleOrderLinesOut, VSIConstants.ELE_ORDER_LINE);
					String strLineNumber=eleShpmntLn.getAttribute(VSIConstants.ATTR_PRIME_LINE_NO);
					String strItemID=eleShpmntLn.getAttribute(VSIConstants.ATTR_ITEM_ID);
					String strItemDesc=eleShpmntLn.getAttribute(VSIConstants.ATTR_ITEM_DESC);					
					eleOrderLineOut.setAttribute("LineNumber", strLineNumber);				
					putElementValue(eleOrderLineOut,"ItemID", strItemID);								
					Element eleShpmntOrdLn=SCXmlUtil.getChildElement(eleShpmntLn, VSIConstants.ELE_ORDER_LINE);
					String strQuantityOrdered=eleShpmntOrdLn.getAttribute(VSIConstants.ATTR_ORIGINAL_ORDERED_QTY);
					String strQuantityPU=eleShpmntOrdLn.getAttribute(VSIConstants.ATTR_ORD_QTY);
					Element eleItemDtls=SCXmlUtil.getChildElement(eleShpmntOrdLn, VSIConstants.ELE_ITEM_DETAILS);
					Element eleItemAliasList=SCXmlUtil.getChildElement(eleItemDtls, VSIConstants.ELE_ITEM_ALIAS_LIST);
					NodeList nlItemAlias=eleItemAliasList.getElementsByTagName(VSIConstants.ELE_ITEM_ALIAS);
					String strUPC="";
					for(int m=0; m<nlItemAlias.getLength(); m++){
						Element eleItemAlias=(Element) nlItemAlias.item(m);
						String strAliasName=eleItemAlias.getAttribute(VSIConstants.ATTR_ALIAS_NAME);
						if(VSIConstants.ATTR_UPC.equals(strAliasName)){
							String strAliasValue=eleItemAlias.getAttribute(VSIConstants.ATTR_ALIAS_VALUE);
							strUPC=strAliasValue;
							break;
						}
					}
					putElementValue(eleOrderLineOut,"UPC", strUPC);
					putElementValue(eleOrderLineOut,"Description", strItemDesc);
					putElementValue(eleOrderLineOut,"QuantityOrdered", strQuantityOrdered);
					putElementValue(eleOrderLineOut,"QuantityPU", strQuantityPU);
					Element eleLinePriceInfo=SCXmlUtil.getChildElement(eleShpmntOrdLn, VSIConstants.ELE_LINE_PRICE);
					String strUnitPrice=eleLinePriceInfo.getAttribute(VSIConstants.ATTR_UNIT_PRICE);
					putElementValue(eleOrderLineOut,"UnitPrice", strUnitPrice);
					Element eleOrdLnOverallTotals = SCXmlUtil.getChildElement(eleShpmntOrdLn, VSIConstants.ELE_LINE_OVERALL_TOTALS);
					String strExtendedPrice=eleOrdLnOverallTotals.getAttribute(VSIConstants.ATTR_EXTENDED_PRICE);
					putElementValue(eleOrderLineOut,"ExtendedPrice", strExtendedPrice);
					String strLnTtlWOTax=eleOrdLnOverallTotals.getAttribute("LineTotalWithoutTax");
					double dLnTtlWOTax=Double.parseDouble(strLnTtlWOTax);
					dSubTotal=dSubTotal+dLnTtlWOTax;
					String strLnTax=eleOrdLnOverallTotals.getAttribute(VSIConstants.ATTR_TAX);
					double dLnTax=Double.parseDouble(strLnTax);
					dSalesTaxAmnt=dSalesTaxAmnt+dLnTax;
					String strLnTtl=eleOrdLnOverallTotals.getAttribute(VSIConstants.ATTR_LINE_TOTAL);
					
					double dLnTtl=Double.parseDouble(strLnTtl);
					dTotal=dTotal+dLnTtl;
					
					//NewChanges-14Aug2020- START
					//Element eleLnDiscounts=SCXmlUtil.createChild(eleOrderLineOut, "LineDiscounts");
					//NewChanges-14Aug2020- END
					Element eleOrdLnChrgs=SCXmlUtil.getChildElement(eleShpmntOrdLn, VSIConstants.ELE_LINE_CHARGES);
					NodeList nlOrdLnChrg=eleOrdLnChrgs.getElementsByTagName(VSIConstants.ELE_LINE_CHARGE);
					for(int j=0; j<nlOrdLnChrg.getLength(); j++){
						
						Element eleOrdLnChrg = (Element) nlOrdLnChrg.item(j);
						//NewChanges-14Aug2020- START
						//Element eleLnDiscount=SCXmlUtil.createChild(eleLnDiscounts, "LineDiscount");
						//NewChanges-14Aug2020- END
						String strChargeName=eleOrdLnChrg.getAttribute(VSIConstants.ATTR_CHARGE_NAME);
						String strChargeAmount=eleOrdLnChrg.getAttribute(VSIConstants.ATTR_CHARGE_AMOUNT);
						//NewChanges-14Aug2020- START
						putElementValue(eleOrderLineOut,"LineDiscountDescription", strChargeName);
						putElementValue(eleOrderLineOut,"LineDiscountAmount", strChargeAmount);
						//NewChanges-14Aug2020- END
						double dChargeAmount=Double.parseDouble(strChargeAmount);
						dTotalChargeAmount=dTotalChargeAmount+dChargeAmount;
					}
					//NewChanges-14Aug2020- START
					Element eleOrderLineOverAllTotals=SCXmlUtil.getChildElement(eleShpmntOrdLn, VSIConstants.ELE_LINE_OVERALL_TOTALS);
					String LineTax = "";
					String LinesTaxRate="";

					if (eleOrderLineOverAllTotals != null) {
						LineTax= eleOrderLineOverAllTotals.getAttribute(VSIConstants.ATTR_TAX);
							
					}					
					if (eleOrdLnTax != null) {
						LinesTaxRate = eleOrdLnTax.getAttribute("TaxPercentage");	
					}
					putElementValue(eleOrderLineOut,"LineTax", LineTax);
					putElementValue(eleOrderLineOut,"LinesTaxRate", LinesTaxRate);
					putElementValue(eleOrderLineOut,"LineTotal", strLnTtlWOTax);
					//NewChanges-14Aug2020- END
					putElementValue(eleOrderLineOut,"LineYourPrice", strLnTtl);
				}
				
				String strYouSaved=Double.toString(dTotalChargeAmount);
				String strSubtotal=Double.toString(dSubTotal);
				String strSalesTaxAmount=Double.toString(dSalesTaxAmnt);
				String strTotal=Double.toString(dTotal);			
				
				putElementValue(elePickPackXML,"YouSaved", strYouSaved);
				putElementValue(elePickPackXML,"Subtotal", strSubtotal);
				putElementValue(elePickPackXML,"SalesTaxRate", strTaxPercentage);
				putElementValue(elePickPackXML,"SalesTaxAmount", strSalesTaxAmount);
				putElementValue(elePickPackXML,"Total", strTotal);
				//NewChanges-14Aug2020- START
				String strAssociate= env.getUserId();
				log.info("env.getUserId()"+env.getUserId()+" env.getProgId() "+env.getProgId()+" env.getTokenID() "+env.getTokenID());
				putElementValue(elePickPackXML,"Associate", strAssociate);
				//NewChanges-14Aug2020- END
				
				printLogs("Printing PickPack XML :"+SCXmlUtil.getString(docPickPackXML));		
				log.info("Printing PickPack XML :"+SCXmlUtil.getString(docPickPackXML));
							
				invokeMiniSoftWebService(env,docPickPackXML);
				
				updateShipmentFlag(env, strShipmentKey, "ExtnPickPackFlag");
				
				printLogs("================Exiting VSISOMPrintReceiptXML Class and printPickPackReceipt Method================");
				
				return docPickPackXML;
			
			}
			 return inXML;
			
		}
		}catch (YFSException e) {
			e.printStackTrace();
			throw new YFSException();
		} catch (Exception e){
			e.printStackTrace();
			throw new YFSException();
		}		
	}

	private void updateShipmentFlag(YFSEnvironment env, String strShipmentKey, String strAttribute)
			throws ParserConfigurationException, YIFClientCreationException,
			RemoteException {
		
		Document docChangeShipmentIn=XMLUtil.createDocument(VSIConstants.ELE_SHIPMENT);
		Element eleChangeShipmentIn=docChangeShipmentIn.getDocumentElement();
		
		eleChangeShipmentIn.setAttribute(VSIConstants.ATTR_SHIPMENT_KEY, strShipmentKey);
		Element eleShpExtn=SCXmlUtil.createChild(eleChangeShipmentIn, VSIConstants.ELE_EXTN);
		eleShpExtn.setAttribute(strAttribute, VSIConstants.FLAG_Y);
		
		printLogs("changeShipment api Input: "+XMLUtil.getXMLString(docChangeShipmentIn));
		VSIUtils.invokeAPI(env, VSIConstants.API_CHANGE_SHIPMENT, docChangeShipmentIn);
		printLogs("changeShipment api was invoked Successfully");
	}

	private void invokeMiniSoftWebService(YFSEnvironment env, Document docInXML)
			throws Exception {
		
		printLogs("================Inside invokeMiniSoftWebService Method================");
		
		String strMinisoftURL=YFSSystem.getProperty("MINISOFT_BOPUS_WEBSERVICE_URL");
		
		printLogs("URL obtained from COP file: "+strMinisoftURL);		
		
		String inDocString=XMLUtil.getXMLString(docInXML);
		printLogs("inDocString: "+inDocString);
		
		DataOutputStream wr=null;
		
		URL url = new URL (strMinisoftURL);
		
		HttpURLConnection connection =  (HttpURLConnection) url.openConnection();
		
		connection.setRequestMethod("POST");
		byte[] bContent = inDocString.getBytes();
		connection.setRequestProperty("Content-Type", "application/xml;charset=utf-8");
		connection.setRequestProperty("Content-Length", String.valueOf(bContent.length));
		connection.setDefaultUseCaches(false);
		connection.setDoOutput(true);		
		
		printLogs("Invoking the MiniSoft web service");
		
		wr = new DataOutputStream (connection.getOutputStream());
		wr.write(bContent);
		
		printLogs("Invoked the MiniSoft web service");	
		
		wr.flush();
        wr.close();
        
        printLogs("Before reading ResponseMessage");
        
        String responseStatus = connection.getResponseMessage();
        printLogs("responseStatus: "+responseStatus);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(
        		connection.getInputStream()));
        printLogs("After reading InputStream");
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        printLogs("response: " + response.toString());
        
        String strResponse=response.toString();
        
        printLogs("strResponse: " + strResponse);
		
		Document responseDoc=null;		
		
		responseDoc=SCXmlUtil.createFromString(strResponse);				
		
		printLogs("Response from MiniSoft WebService: "+XMLUtil.getXMLString(responseDoc));
		
		printLogs("MiniSoft Request will be stored in DB");
		
		VSIUtils.invokeService(env, "VSISOMPrintXML_DB", docInXML);
		
		printLogs("MiniSoft Request is stored in DB");
		
		printLogs("MiniSoft Response will be stored in DB");
		
		VSIUtils.invokeService(env, "VSISOMPrintXML_DB", responseDoc);
		
		printLogs("MiniSoft Response is stored in DB");
		
		printLogs("================Exiting invokeMiniSoftWebService Method================");
	}
	
	public Document printCustomerReceipt(YFSEnvironment env, Document inXML){
		
		printLogs("================Inside VSISOMPrintReceiptXML Class and printCustomerReceipt Method================");
		printLogs("Printing Input XML :"+SCXmlUtil.getString(inXML));
		
		try{
			
			Element eleShipment=inXML.getDocumentElement();			
			String strCustRcptFlag=null;			
			String strShipmentKey =eleShipment.getAttribute(VSIConstants.ATTR_SHIPMENT_KEY);
			
			Document docGetShipmentListIn=XMLUtil.createDocument(VSIConstants.ELE_SHIPMENT);
			Element eleGetShipmentList = docGetShipmentListIn.getDocumentElement();
			eleGetShipmentList.setAttribute(VSIConstants.ATTR_SHIPMENT_KEY, strShipmentKey);
			
			log.info("Printing Input XML to API_GET_SHIPMENT_LIST CUSTOMER RECEIPT:"+SCXmlUtil.getString(docGetShipmentListIn));
			
			Document docGetShipmentListOut = VSIUtils.invokeAPI(env,VSIConstants.SHIPMENT_LIST_PICK_PACK_TEMPLATE,VSIConstants.API_GET_SHIPMENT_LIST, docGetShipmentListIn);
			log.info("Printing Output XML to API_GET_SHIPMENT_LIST CUSTOMER RECEIPT:"+SCXmlUtil.getString(docGetShipmentListOut));
			
			Element eleGetShipmentListOut = (Element) docGetShipmentListOut.getElementsByTagName(VSIConstants.ELE_SHIPMENTS).item(0);			
			int intTotalNoOfShipment= Integer.parseInt(eleGetShipmentListOut.getAttribute(VSIConstants.ATTR_TOTAL_NUMBER_OF_RECORDS));
			Element eleShipmentOut=null;
			Element eleShpmntExtn= null;
			String strShipmentStatus = null;
			if (intTotalNoOfShipment > 0) {
				 eleShipmentOut= (Element) docGetShipmentListOut.getElementsByTagName(VSIConstants.ELE_SHIPMENT).item(0);
				 eleShpmntExtn=SCXmlUtil.getChildElement(eleShipmentOut, VSIConstants.ELE_EXTN);
				 
				 if(!YFCCommon.isVoid(eleShpmntExtn)){
						strCustRcptFlag=eleShpmntExtn.getAttribute("ExtnCustomerReceiptFlag");
				 }
				 strShipmentStatus= eleShipmentOut.getAttribute(VSIConstants.ATTR_STATUS);
			}
			
			log.info("Printing Customer Receipt Flag"+strCustRcptFlag);
			log.info("Shipment Status for Customer Receipt"+strCustRcptFlag);
			
			if(VSIConstants.FLAG_Y.equals(strCustRcptFlag)){
				printLogs("CustomerReceipt Process has already been completed for this Shipment");
				printLogs("Exiting VSISOMPrintReceiptXML Class and printCustomerReceipt Method");
				return inXML;
			}else{
			 if(strShipmentStatus.equalsIgnoreCase("1400")) {
				 
				printLogs("Starting CustomerReceipt Process for this Shipment");
			
				Document docCustomerReceiptXML=XMLUtil.createDocument("OrderData");
				Element eleCustomerReceiptXML=docCustomerReceiptXML.getDocumentElement();			
				Document docSortedShpmntIn = XMLUtil.createDocument(VSIConstants.ELE_SHIPMENT);
				Element eleSortedShpmntIn = docSortedShpmntIn.getDocumentElement();
				eleSortedShpmntIn.setAttribute(VSIConstants.ATTR_SHIPMENT_KEY, strShipmentKey);
				
				printLogs("Input to getSortedShipmentDetails API: "+SCXmlUtil.getString(docSortedShpmntIn));
				log.info("Input to getSortedShipmentDetails API: "+SCXmlUtil.getString(docSortedShpmntIn));
				Document docSortedShpmntOut = VSIUtils.invokeAPI(env,"global/template/api/VSISOMCustomerShipmentSorted.xml","getSortedShipmentDetails", docSortedShpmntIn);
				printLogs("Output from getSortedShipmentDetails API: "+SCXmlUtil.getString(docSortedShpmntOut));
				log.info("Output from getSortedShipmentDetails API: "+SCXmlUtil.getString(docSortedShpmntOut));
			
				Element eleSortedShpmntOut=docSortedShpmntOut.getDocumentElement();
				String strStoreNo=eleSortedShpmntOut.getAttribute(VSIConstants.ATTR_SHIP_NODE);
				String strStoreNumber=("0000" + strStoreNo).substring(strStoreNo.length());			
				String strOrderNumber=eleSortedShpmntOut.getAttribute(VSIConstants.ATTR_SHIP_CUST_PO_NO);
				Element eleFromAddr = SCXmlUtil.getChildElement(eleSortedShpmntOut, "FromAddress");
				String strStoreAddressLine1=eleFromAddr.getAttribute(VSIConstants.ATTR_ADDRESS1);
				String strStoreAddressLine2=eleFromAddr.getAttribute(VSIConstants.ATTR_ADDRESS2);
				String strStoreCity=eleFromAddr.getAttribute(VSIConstants.ATTR_CITY);
				String strStoreState=eleFromAddr.getAttribute(VSIConstants.ATTR_STATE);
				String strStoreZipCode=eleFromAddr.getAttribute(VSIConstants.ATTR_ZIPCODE);
				String strStorePhoneNumber=eleFromAddr.getAttribute(VSIConstants.ATTR_DAY_PHONE);
				
				Element eleShipmentLines=SCXmlUtil.getChildElement(eleSortedShpmntOut, VSIConstants.ELE_SHIPMENT_LINES);
				
				Element eleShipmentLine=SCXmlUtil.getChildElement(eleShipmentLines, VSIConstants.ELE_SHIPMENT_LINE);
				
				Element eleOrder=SCXmlUtil.getChildElement(eleShipmentLine, VSIConstants.ELE_ORDER);
				String strCustomerName=eleOrder.getAttribute(VSIConstants.ATTR_CUSTOMER_FIRST_NAME);
				String strCustomerNumber=eleOrder.getAttribute(VSIConstants.ATTR_BILL_TO_ID);
				//NewChanges-14Aug2020- START		
				Element eleBillTo = SCXmlUtil.getChildElement(eleSortedShpmntOut, VSIConstants.ELE_BILL_TO_ADDRESS);
				String strCustomerEmail=eleBillTo.getAttribute(VSIConstants.ATTR_EMAIL_ID);
				String strCustomerAdd1=eleBillTo.getAttribute(VSIConstants.ATTR_ADDRESS1);
				String strCustomerAdd2=eleBillTo.getAttribute(VSIConstants.ATTR_ADDRESS2);
				String strCustomerCity=eleBillTo.getAttribute(VSIConstants.ATTR_CITY);
				String strCustomerState=eleBillTo.getAttribute(VSIConstants.ATTR_STATE);
				String strCustomerZipCode=eleBillTo.getAttribute(VSIConstants.ATTR_ZIPCODE);
				String strCustomerPhoneNumber=eleBillTo.getAttribute(VSIConstants.ATTR_DAY_PHONE);
				//NewChanges-14Aug2020- END
				String strEntryType=eleOrder.getAttribute(VSIConstants.ATTR_ENTRY_TYPE);			
				Element elePaymentMethods=SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_PAYMENT_METHODS);
				Element eleOrderExtn=SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_EXTN);
				String strMembershipStatus=eleOrderExtn.getAttribute(VSIConstants.ATTR_CUSTOMER_TIER);
				String strPointsEarned=eleOrderExtn.getAttribute("ExtnPointsEarned");
				String strTotalPointsEarned=eleOrderExtn.getAttribute("ExtnTotalPointsEarned");
				String strOrderPoints="";
				String strCurrentBalance="";
				if(VSIConstants.WEB.equals(strEntryType)){
					if(!YFCCommon.isVoid(strPointsEarned)){
						strOrderPoints=strPointsEarned;
					}
					if(!YFCCommon.isVoid(strTotalPointsEarned)){
						strCurrentBalance=strTotalPointsEarned;
					}
				}else if(VSIConstants.ENTRYTYPE_CC.equals(strEntryType)){
					if(!YFCCommon.isVoid(strTotalPointsEarned)){
						strOrderPoints=strTotalPointsEarned;
					}
					if(!YFCCommon.isVoid(strPointsEarned)){
						strCurrentBalance=strPointsEarned;
					}
				}
				Element eleOrderLine=SCXmlUtil.getChildElement(eleShipmentLine, VSIConstants.ELE_ORDER_LINE);
				Element eleOrdLnTaxes=SCXmlUtil.getChildElement(eleOrderLine, VSIConstants.ELE_LINE_TAXES);
				Element eleOrdLnTax=SCXmlUtil.getChildElement(eleOrdLnTaxes, VSIConstants.ELE_LINE_TAX);
				String strTaxPercentage="";
				
				if (eleOrdLnTax != null) {
					 strTaxPercentage=eleOrdLnTax.getAttribute("TaxPercentage");
				}
				
				String strDocType = getCommonCodeValues(env,"SOM_DOCUMENT_TYPES","CustomerReceipt");
				
				String strFooterMessage = getCommonCodeValues(env,"SOM_FOOTER_MESSAGE","FOOTERMESSAGE");
				
				putElementValue(eleCustomerReceiptXML,"StoreNumber", strStoreNumber);
				putElementValue(eleCustomerReceiptXML,"StoreAddressLine1", strStoreAddressLine1);
				putElementValue(eleCustomerReceiptXML,"StoreAddressLine2", strStoreAddressLine2);
				putElementValue(eleCustomerReceiptXML,"StoreCity", strStoreCity);
				putElementValue(eleCustomerReceiptXML,"StoreState", strStoreState);
				putElementValue(eleCustomerReceiptXML,"StoreZipCode", strStoreZipCode);
				putElementValue(eleCustomerReceiptXML,"StorePhoneNumber", strStorePhoneNumber);
				putElementValue(eleCustomerReceiptXML,"DocumentType", strDocType);
				//NewChanges-14Aug2020- START
				putElementValue(eleCustomerReceiptXML,"CustomerName", strCustomerName);
				putElementValue(eleCustomerReceiptXML,"CustomerEmail", strCustomerEmail);
				putElementValue(eleCustomerReceiptXML,"CustomerAddressLine1", strCustomerAdd1);
				putElementValue(eleCustomerReceiptXML,"CustomerAddressLine2", strCustomerAdd2);
				putElementValue(eleCustomerReceiptXML,"CustomerCity", strCustomerCity);
				putElementValue(eleCustomerReceiptXML,"CustomerState", strCustomerState);
				putElementValue(eleCustomerReceiptXML,"CustomerZipCode", strCustomerZipCode);			
				putElementValue(eleCustomerReceiptXML,"CustomerNumber", strCustomerNumber);
				putElementValue(eleCustomerReceiptXML,"CustomerPhoneNumber", strCustomerPhoneNumber);
				//NewChanges-14Aug2020- END
				putElementValue(eleCustomerReceiptXML,"CustomerName", strCustomerName);
				putElementValue(eleCustomerReceiptXML,"CustomerNumber", strCustomerNumber);
				putElementValue(eleCustomerReceiptXML,"OrderNumber", strOrderNumber);
				
				Element eleShipNode = SCXmlUtil.getChildElement(eleSortedShpmntOut, VSIConstants.ELE_SHIP_NODE);
				String strLocalecode=eleShipNode.getAttribute("Localecode");
				String strTimeZone="";
				if(VSIConstants.ATTR_EN_US_EST.equals(strLocalecode)){
					strTimeZone="US/Eastern";
				}else if("en_US_CST".equals(strLocalecode)){
					strTimeZone="US/Central";
				}else if("en_US_PST".equals(strLocalecode)){
					strTimeZone="US/Pacific";
				}else if("en_US_HAST".equals(strLocalecode)){
					strTimeZone="US/Hawaii";
				}else if("en_US_MST".equals(strLocalecode)){
					strTimeZone="US/Mountain";
				}else if("en_US_MDT".equals(strLocalecode)){
					strTimeZone="US/Mountain";
				}
				
				Calendar calendar = Calendar.getInstance(); 
				
				SimpleDateFormat sysDt = new SimpleDateFormat("dd MMM yyyy");
		        sysDt.setTimeZone(TimeZone.getTimeZone(strTimeZone));
		        String sysDate = sysDt.format(calendar.getTime());
		        
		        SimpleDateFormat sysTm = new SimpleDateFormat("HH:mm:ss z");
		        sysTm.setTimeZone(TimeZone.getTimeZone(strTimeZone));
		        String sysTime = sysTm.format(calendar.getTime());
		        
		        Element elePayments=SCXmlUtil.createChild(eleCustomerReceiptXML, "Payments");
				NodeList nlPaymentMethod=elePaymentMethods.getElementsByTagName(VSIConstants.ELE_PAYMENT_METHOD);
				for(int k=0; k<nlPaymentMethod.getLength(); k++){
					Element elePaymentMethod=(Element)nlPaymentMethod.item(k);
					Element elePayment=SCXmlUtil.createChild(elePayments, "Payment");
					String strPaymentType=elePaymentMethod.getAttribute(VSIConstants.ATTR_PAYMENT_TYPE);
					String strPaymentMethod="";
					String strPaymentDetail="";
					String strPaymentAmount="";
					if(VSIConstants.PAYMENT_MODE_CC.equals(strPaymentType)){
						strPaymentMethod=elePaymentMethod.getAttribute(VSIConstants.ATTR_CREDIT_CARD_TYPE);
						strPaymentDetail=elePaymentMethod.getAttribute("CreditCardNo");
						//NewChanges-14Aug2020- START
						//strPaymentAmount=elePaymentMethod.getAttribute("RequestedChargeAmount");
						//strPaymentDetail=strPaymentDetail.substring(strPaymentDetail.length()-4);
						//NewChanges-14Aug2020- END
					}else if(VSIConstants.PAYMENT_MODE_GC.equals(strPaymentType)){
						strPaymentMethod="Gift Card";
						strPaymentDetail=elePaymentMethod.getAttribute("DisplaySvcNo");
					}else if(VSIConstants.PAYMENT_MODE_VOUCHERS.equals(strPaymentType)){
						strPaymentMethod="Health Awards";
						strPaymentDetail=elePaymentMethod.getAttribute(VSIConstants.ATTR_PAYMENT_REFERENCE_1);
					}else if(VSIConstants.PAYMENT_MODE_PP.equals(strPaymentType)){
						strPaymentMethod="PayPal";
						strPaymentDetail=elePaymentMethod.getAttribute(VSIConstants.ATTR_PAYMENT_REFERENCE_3);
					}
					putElementValue(elePayment,"PaymentMethod", strPaymentMethod);
					putElementValue(elePayment,"PaymentDetail", strPaymentDetail);
					//NewChanges-14Aug2020- START
					putElementValue(elePayment,"PaymentAmount", strPaymentAmount);
					//NewChanges-14Aug2020- END
				}
				
				putElementValue(eleCustomerReceiptXML,"MembershipStatus", strMembershipStatus);
				putElementValue(eleCustomerReceiptXML,"OrderPoints", strOrderPoints);
				putElementValue(eleCustomerReceiptXML,"CurrentBalance", strCurrentBalance);
				putElementValue(eleCustomerReceiptXML,"TranNumber", "");
				putElementValue(eleCustomerReceiptXML,"Date", sysDate);
				putElementValue(eleCustomerReceiptXML,"Time", sysTime);
				//putElementValue(eleCustomerReceiptXML,"Associate", "");
				putElementValue(eleCustomerReceiptXML,"FooterMessage", strFooterMessage);
							
				
				Element eleOrderLinesOut=SCXmlUtil.createChild(eleCustomerReceiptXML, VSIConstants.ELE_ORDER_LINES);
				NodeList nlShipmentLine=eleShipmentLines.getElementsByTagName(VSIConstants.ELE_SHIPMENT_LINE);
				double dTotalChargeAmount=0.0;
				double dSubTotal=0.0;
				double dSalesTaxAmnt=0.0;
				double dTotal=0.0;
				for(int i=0; i<nlShipmentLine.getLength(); i++){
					Element eleShpmntLn = (Element) nlShipmentLine.item(i);
					Element eleOrderLineOut=SCXmlUtil.createChild(eleOrderLinesOut, VSIConstants.ELE_ORDER_LINE);
					String strLineNumber=eleShpmntLn.getAttribute(VSIConstants.ATTR_PRIME_LINE_NO);
					String strItemID=eleShpmntLn.getAttribute(VSIConstants.ATTR_ITEM_ID);
					String strItemDesc=eleShpmntLn.getAttribute(VSIConstants.ATTR_ITEM_DESC);					
					eleOrderLineOut.setAttribute("LineNumber", strLineNumber);				
					putElementValue(eleOrderLineOut,"ItemID", strItemID);								
					Element eleShpmntOrdLn=SCXmlUtil.getChildElement(eleShpmntLn, VSIConstants.ELE_ORDER_LINE);
					String strQuantityOrdered=eleShpmntOrdLn.getAttribute(VSIConstants.ATTR_ORIGINAL_ORDERED_QTY);
					String strQuantityPU=eleShpmntOrdLn.getAttribute(VSIConstants.ATTR_ORD_QTY);
					Element eleItemDtls=SCXmlUtil.getChildElement(eleShpmntOrdLn, VSIConstants.ELE_ITEM_DETAILS);
					Element eleItemAliasList=SCXmlUtil.getChildElement(eleItemDtls, VSIConstants.ELE_ITEM_ALIAS_LIST);
					NodeList nlItemAlias=eleItemAliasList.getElementsByTagName(VSIConstants.ELE_ITEM_ALIAS);
					String strUPC="";
					for(int m=0; m<nlItemAlias.getLength(); m++){
						Element eleItemAlias=(Element) nlItemAlias.item(m);
						String strAliasName=eleItemAlias.getAttribute(VSIConstants.ATTR_ALIAS_NAME);
						if(VSIConstants.ATTR_UPC.equals(strAliasName)){
							String strAliasValue=eleItemAlias.getAttribute(VSIConstants.ATTR_ALIAS_VALUE);
							strUPC=strAliasValue;
							break;
						}
					}
					putElementValue(eleOrderLineOut,"UPC", strUPC);
					putElementValue(eleOrderLineOut,"Description", strItemDesc);
					putElementValue(eleOrderLineOut,"QuantityOrdered", strQuantityOrdered);
					putElementValue(eleOrderLineOut,"QuantityPU", strQuantityPU);
					Element eleLinePriceInfo=SCXmlUtil.getChildElement(eleShpmntOrdLn, VSIConstants.ELE_LINE_PRICE);
					String strUnitPrice=eleLinePriceInfo.getAttribute(VSIConstants.ATTR_UNIT_PRICE);
					putElementValue(eleOrderLineOut,"UnitPrice", strUnitPrice);
					Element eleOrdLnOverallTotals = SCXmlUtil.getChildElement(eleShpmntOrdLn, VSIConstants.ELE_LINE_OVERALL_TOTALS);
					String strExtendedPrice=eleOrdLnOverallTotals.getAttribute(VSIConstants.ATTR_EXTENDED_PRICE);
					putElementValue(eleOrderLineOut,"ExtendedPrice", strExtendedPrice);
					String strLnTtlWOTax=eleOrdLnOverallTotals.getAttribute("LineTotalWithoutTax");
					double dLnTtlWOTax=Double.parseDouble(strLnTtlWOTax);
					dSubTotal=dSubTotal+dLnTtlWOTax;
					String strLnTax=eleOrdLnOverallTotals.getAttribute(VSIConstants.ATTR_TAX);
					double dLnTax=Double.parseDouble(strLnTax);
					dSalesTaxAmnt=dSalesTaxAmnt+dLnTax;
					String strLnTtl=eleOrdLnOverallTotals.getAttribute(VSIConstants.ATTR_LINE_TOTAL);
					
					double dLnTtl=Double.parseDouble(strLnTtl);
					dTotal=dTotal+dLnTtl;
					
					//NewChanges-14Aug2020- START
					//Element eleLnDiscounts=SCXmlUtil.createChild(eleOrderLineOut, "LineDiscounts");
					//NewChanges-14Aug2020- END
					Element eleOrdLnChrgs=SCXmlUtil.getChildElement(eleShpmntOrdLn, VSIConstants.ELE_LINE_CHARGES);
					NodeList nlOrdLnChrg=eleOrdLnChrgs.getElementsByTagName(VSIConstants.ELE_LINE_CHARGE);
					for(int j=0; j<nlOrdLnChrg.getLength(); j++){
						
						Element eleOrdLnChrg = (Element) nlOrdLnChrg.item(j);
						//NewChanges-14Aug2020- START
						//Element eleLnDiscount=SCXmlUtil.createChild(eleLnDiscounts, "LineDiscount");
						//NewChanges-14Aug2020- END
						String strChargeName=eleOrdLnChrg.getAttribute(VSIConstants.ATTR_CHARGE_NAME);
						String strChargeAmount=eleOrdLnChrg.getAttribute(VSIConstants.ATTR_CHARGE_AMOUNT);
						//NewChanges-14Aug2020- START
						putElementValue(eleOrderLineOut,"LineDiscountDescription", strChargeName);
						putElementValue(eleOrderLineOut,"LineDiscountAmount", strChargeAmount);
						//NewChanges-14Aug2020- END
						double dChargeAmount=Double.parseDouble(strChargeAmount);
						dTotalChargeAmount=dTotalChargeAmount+dChargeAmount;
					}
					//NewChanges-14Aug2020- START
					Element eleOrderLineOverAllTotals=SCXmlUtil.getChildElement(eleShpmntOrdLn, VSIConstants.ELE_LINE_OVERALL_TOTALS);
					String LineTax = "";
					String LinesTaxRate="";

					if (eleOrderLineOverAllTotals != null) {
						LineTax= eleOrderLineOverAllTotals.getAttribute(VSIConstants.ATTR_TAX);
							
					}					
					if (eleOrdLnTax != null) {
						LinesTaxRate = eleOrdLnTax.getAttribute("TaxPercentage");	
					}
					putElementValue(eleOrderLineOut,"LineTax", LineTax);
					putElementValue(eleOrderLineOut,"LinesTaxRate", LinesTaxRate);
					putElementValue(eleOrderLineOut,"LineTotal", strLnTtlWOTax);
					//NewChanges-14Aug2020- END
					putElementValue(eleOrderLineOut,"LineYourPrice", strLnTtl);
				}
				
				String strYouSaved=Double.toString(dTotalChargeAmount);
				String strSubtotal=Double.toString(dSubTotal);
				String strSalesTaxAmount=Double.toString(dSalesTaxAmnt);
				String strTotal=Double.toString(dTotal);			
				
				putElementValue(eleCustomerReceiptXML,"YouSaved", strYouSaved);
				putElementValue(eleCustomerReceiptXML,"Subtotal", strSubtotal);
				putElementValue(eleCustomerReceiptXML,"SalesTaxRate", strTaxPercentage);
				putElementValue(eleCustomerReceiptXML,"SalesTaxAmount", strSalesTaxAmount);
				putElementValue(eleCustomerReceiptXML,"Total", strTotal);
				//NewChanges-14Aug2020- START
				String strAssociate= env.getUserId();
				log.info("env.getUserId()"+env.getUserId()+" env.getProgId() "+env.getProgId()+" env.getTokenID() "+env.getTokenID());
				putElementValue(eleCustomerReceiptXML,"Associate", strAssociate);
				//NewChanges-14Aug2020- END
				
				printLogs("Printing CustomerReceipt XML :"+SCXmlUtil.getString(docCustomerReceiptXML));
				
				invokeMiniSoftWebService(env,docCustomerReceiptXML);
				
				updateShipmentFlag(env, strShipmentKey, "ExtnCustomerReceiptFlag");
				
				printLogs("================Exiting VSISOMPrintReceiptXML Class and printCustomerReceipt Method================");
				
				return docCustomerReceiptXML;
			
			}
			 return inXML;
			}
			
		}catch (YFSException e) {
			e.printStackTrace();
			throw new YFSException();
		} catch (Exception e){
			e.printStackTrace();
			throw new YFSException();
		}		
	}

	private String getCommonCodeValues(YFSEnvironment env, String strCodeType, String strCodeValue)
			throws ParserConfigurationException, YIFClientCreationException,
			RemoteException {
		
		Document docInputgetCommonCodeList=XMLUtil.createDocument(VSIConstants.ELE_COMMON_CODE);
		Element eleCommonCode = docInputgetCommonCodeList.getDocumentElement();
		eleCommonCode.setAttribute(VSIConstants.ATTR_CODE_TYPE, strCodeType);
		eleCommonCode.setAttribute(VSIConstants.ATTR_CODE_VALUE, strCodeValue);
		
		printLogs("Input for getCommonCodeList :" +XMLUtil.getXMLString(docInputgetCommonCodeList));
		Document docCommonCodeListOP=VSIUtils.invokeAPI(env,VSIConstants.API_COMMON_CODE_LIST, docInputgetCommonCodeList);
		printLogs("Output from getCommonCodeList :" +XMLUtil.getXMLString(docCommonCodeListOP));
		
		Element eleCCOut=(Element)docCommonCodeListOP
				.getElementsByTagName(VSIConstants.ELE_COMMON_CODE).item(0);
		
		String strCCValue=eleCCOut.getAttribute(VSIConstants.ATTR_CODE_SHORT_DESCRIPTION);
		
		return strCCValue;
	}
	
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}
	
	private void putElementValue(Element childEle, String key, Object value) {
		Element ele = SCXmlUtil.createChild(childEle, key);
		if(value instanceof String ) {
			ele.setTextContent((String)value);
		}else if(value instanceof Element ) {
			ele.appendChild((Element)value);
		}
	}
}
