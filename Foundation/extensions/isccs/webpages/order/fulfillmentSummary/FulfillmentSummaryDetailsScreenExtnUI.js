
scDefine(["dojo/text!./templates/FulfillmentSummaryDetailsScreenExtn.html","scbase/loader!dojo/_base/declare","scbase/loader!dojo/_base/kernel","scbase/loader!dojo/_base/lang","scbase/loader!dojo/text","scbase/loader!sc/plat","scbase/loader!sc/plat/dojo/utils/BaseUtils"]
 , function(			 
			    templateText
			 ,
			    _dojodeclare
			 ,
			    _dojokernel
			 ,
			    _dojolang
			 ,
			    _dojotext
			 ,
			    _scplat
			 ,
			    _scBaseUtils
){
return _dojodeclare("extn.order.fulfillmentSummary.FulfillmentSummaryDetailsScreenExtnUI",
				[], {
			templateString: templateText
	
	
	
	
	
	
	
	
	,
	hotKeys: [ 
	]

,events : [
	]

,subscribers : {

local : [

{
	  eventId: 'afterScreenInit'

,	  sequence: '51'




,handler : {
methodName : "extn_InitScreen"

 
}
}
,
{
	  eventId: 'saveCurrentPage'

,	  sequence: '19'

,	  description: 'checkFulfillmentOptions'



,handler : {
methodName : "checkFulfillmentOptions"

 
}
}
]
}

});
});


