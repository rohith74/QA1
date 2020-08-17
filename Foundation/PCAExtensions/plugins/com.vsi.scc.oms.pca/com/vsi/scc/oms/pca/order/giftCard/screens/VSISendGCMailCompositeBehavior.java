/*
 * Created on Aug 26,2015
 *
 */
package com.vsi.scc.oms.pca.order.giftCard.screens;

import org.eclipse.swt.widgets.Composite;

import com.vsi.scc.oms.pca.util.VSIXmlUtils;
import com.yantra.yfc.rcp.YRCApiContext;
import com.yantra.yfc.rcp.YRCBehavior;
import com.yantra.yfc.rcp.YRCDesktopUI;
import com.yantra.yfc.rcp.YRCPlatformUI;

/**
 * @author DELL
 * 
 * Generated by Sterling RCP Tools
 */

public class VSISendGCMailCompositeBehavior extends YRCBehavior {

	/**
	 * Constructor for the behavior class.
	 */
	VSISendGCMailComposite compo;

	public VSISendGCMailCompositeBehavior(Composite ownerComposite, String formId) {
		super(ownerComposite, formId);
		compo = (VSISendGCMailComposite) ownerComposite;
		init();
	}

	/**
	 * This method initializes the behavior class.
	 */
	public void init() {
		// TODO: write behavior init here
	}

	public void InsertIntoDB(String xml) {
		if (!YRCPlatformUI.isVoid(xml)) {
			YRCApiContext ctx = new YRCApiContext();
			ctx.setApiName("VSITriggerEmailOnVGCRefund");
			ctx.setFormId(getFormId());
			ctx.setInputXml(VSIXmlUtils.convertStringToDocument(xml));
			this.callApi(ctx);

			this.doClose();
			YRCPlatformUI.showInformation("Gift Card Mail sent",
					"Email is sent successfully!");

		}
	}

	public void doClose() {

		YRCDesktopUI.getCurrentPage().getShell().close();
	}
}