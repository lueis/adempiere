/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.process.WTCDocAction;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;
import org.wtc.util.WTCTimeUtil;
import org.wtc.util.WTCUtil;

/**
 *	Inventory Movement Model
 *	
 *  @author Jorg Janke
 *  @author victor.perez@e-evolution.com, e-Evolution http://www.e-evolution.com
 * 			<li>FR [ 1948157  ]  Is necessary the reference for document reverse
 *  		@see http://sourceforge.net/tracker/?func=detail&atid=879335&aid=1948157&group_id=176962
 * 			<li> FR [ 2520591 ] Support multiples calendar for Org 
 *			@see http://sourceforge.net/tracker2/?func=detail&atid=879335&aid=2520591&group_id=176962 
 *  @author Armen Rizal, Goodwill Consulting
 * 			<li>BF [ 1745154 ] Cost in Reversing Material Related Docs  
 *  @author Teo Sarca, www.arhipac.ro
 *  		<li>FR [ 2214883 ] Remove SQL code and Replace for Query
 *  @version $Id: MMovement.java 1009 2012-02-09 09:16:13Z suman $
 *  
 *  @author				@changeID		 @IssueID		@Description 
 *  PhaniKiran.Gutha	20111226913		 1673			Material Costing Enhancements
 */
public class MMovement extends X_M_Movement implements DocAction , WTCDocAction
{
// Kindly do not delete below line as it is being used for svn version maintenance
public static final String svnRevision =  "$Id: MMovement.java 1009 2012-02-09 09:16:13Z suman $";
	/**
	 * 
	 */
	private static final long serialVersionUID = 3634169801280239573L;

	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param M_Movement_ID id
	 *	@param trxName transaction
	 */
	public MMovement (Properties ctx, int M_Movement_ID, String trxName)
	{
		super (ctx, M_Movement_ID, trxName);
		if (M_Movement_ID == 0)
		{
		//	setC_DocType_ID (0);
			setDocAction (DOCACTION_Complete);	// CO
			setDocStatus (DOCSTATUS_Drafted);	// DR
			setIsApproved (false);
			setIsInTransit (false);
			setMovementDate (new Timestamp(System.currentTimeMillis()));	// @#Date@
			setPosted (false);
			super.setProcessed (false);
		}	
	}	//	MMovement

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MMovement (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MMovement

	/**	Lines						*/
	private MMovementLine[]		m_lines = null;
	/** Confirmations				*/
	private MMovementConfirm[]	m_confirms = null;
	
	/**
	 * 	Get Lines
	 *	@param requery requery
	 *	@return array of lines
	 */
	public MMovementLine[] getLines (boolean requery)
	{
		if (m_lines != null && !requery) {
			set_TrxName(m_lines, get_TrxName());
			return m_lines;
		}
		//
		final String whereClause = "M_Movement_ID=?";
		List<MMovement> list = new Query(getCtx(), MMovementLine.Table_Name, whereClause, get_TrxName())
		 										.setParameters(new Object[]{getM_Movement_ID()})
		 										.setOrderBy(MMovementLine.COLUMNNAME_Line)
		 										.list();
		m_lines = new MMovementLine[list.size ()];
		list.toArray (m_lines);
		return m_lines;
	}	//	getLines

	/**
	 * 	Get Confirmations
	 * 	@param requery requery
	 *	@return array of Confirmations
	 */
	public MMovementConfirm[] getConfirmations(boolean requery)
	{
		if (m_confirms != null && !requery)
			return m_confirms;

		List<MMovementConfirm> list = new Query(getCtx(), MMovementConfirm.Table_Name, "M_Movement_ID=?", get_TrxName())
										.setParameters(new Object[]{get_ID()})
										.list();
		m_confirms = list.toArray(new MMovementConfirm[list.size()]);
		return m_confirms;
	}	//	getConfirmations

	/**
	 * 	Add to Description
	 *	@param description text
	 */
	public void addDescription (String description)
	{
		String desc = getDescription();
		if (desc == null)
			setDescription(description);
		else
			setDescription(desc + " | " + description);
	}	//	addDescription
	
	/**
	 * 	Get Document Info
	 *	@return document info (untranslated)
	 */
	public String getDocumentInfo()
	{
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		return dt.getName() + " " + getDocumentNo();
	}	//	getDocumentInfo

	/**
	 * 	Create PDF
	 *	@return File or null
	 */
	public File createPDF ()
	{
		try
		{
			File temp = File.createTempFile(get_TableName()+get_ID()+"_", ".pdf");
			return createPDF (temp);
		}
		catch (Exception e)
		{
			log.severe("Could not create PDF - " + e.getMessage());
		}
		return null;
	}	//	getPDF

	/**
	 * 	Create PDF file
	 *	@param file output file
	 *	@return file if success
	 */
	public File createPDF (File file)
	{
	//	ReportEngine re = ReportEngine.get (getCtx(), ReportEngine.INVOICE, getC_Invoice_ID());
	//	if (re == null)
			return null;
	//	return re.getPDF(file);
	}	//	createPDF

	
	/**
	 * 	Before Save
	 *	@param newRecord new
	 *	@return true
	 */
	protected boolean beforeSave (boolean newRecord)
	{
		if (getC_DocType_ID() == 0)
		{
			MDocType types[] = MDocType.getOfDocBaseType(getCtx(), MDocType.DOCBASETYPE_MaterialMovement);
			if (types.length > 0)	//	get first
				setC_DocType_ID(types[0].getC_DocType_ID());
			else
			{
				log.saveError("Error", Msg.parseTranslation(getCtx(), "@NotFound@ @C_DocType_ID@"));
				return false;
			}
		}
		return true;
	}	//	beforeSave
	
	/**
	 * 	Set Processed.
	 * 	Propergate to Lines/Taxes
	 *	@param processed processed
	 */
	@Override
	public void setProcessed (boolean processed)
	{
		super.setProcessed (processed);
		if (get_ID() == 0)
			return;
		final String sql = "UPDATE M_MovementLine SET Processed=? WHERE M_Movement_ID=?";
		int noLine = DB.executeUpdateEx(sql, new Object[]{processed, get_ID()}, get_TrxName());
		m_lines = null;
		log.fine("Processed=" + processed + " - Lines=" + noLine);
	}	//	setProcessed
	
	
	/**************************************************************************
	 * 	Process document
	 *	@param processAction document action
	 *	@return true if performed
	 */
	public boolean processIt (String processAction)
	{
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine (this, getDocStatus());
		return engine.processIt (processAction, getDocAction());
	}	//	processIt
	
	/**	Process Message 			*/
	private String		m_processMsg = null;
	/**	Just Prepared Flag			*/
	private boolean		m_justPrepared = false;

	/**
	 * 	Unlock Document.
	 * 	@return true if success 
	 */
	public boolean unlockIt()
	{
		log.info(toString());
		setProcessing(false);
		return true;
	}	//	unlockIt
	
	/**
	 * 	Invalidate Document
	 * 	@return true if success 
	 */
	public boolean invalidateIt()
	{
		log.info(toString());
		setDocAction(DOCACTION_Prepare);
		return true;
	}	//	invalidateIt
	
	/**
	 *	Prepare Document
	 * 	@return new status (In Progress or Invalid) 
	 */
	public String prepareIt()
	{
		log.info(toString());
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());

		//	Std Period open?
		if (!MPeriod.isOpen(getCtx(), getMovementDate(), dt.getDocBaseType(), getAD_Org_ID()))
		{
			m_processMsg = "@PeriodClosed@";
			return DocAction.STATUS_Invalid;
		}
		MMovementLine[] lines = getLines(false);
		if (lines.length == 0)
		{
			m_processMsg = "@NoLines@";
			return DocAction.STATUS_Invalid;
		}
		
		//	Confirmation
		if (dt.isInTransit())
			createConfirmation();

		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		
		m_justPrepared = true;
		if (!DOCACTION_Complete.equals(getDocAction()))
			setDocAction(DOCACTION_Complete);
		return DocAction.STATUS_InProgress;
	}	//	prepareIt
	
	/**
	 * 	Create Movement Confirmation
	 */
	private void createConfirmation()
	{
		MMovementConfirm[] confirmations = getConfirmations(false);
		if (confirmations.length > 0)
			return;
		
		//	Create Confirmation
		MMovementConfirm.create (this, false);
	}	//	createConfirmation
	
	/**
	 * 	Approve Document
	 * 	@return true if success 
	 */
	public boolean  approveIt()
	{
		log.info(toString());
		setIsApproved(true);
		return true;
	}	//	approveIt
	
	/**
	 * 	Reject Approval
	 * 	@return true if success 
	 */
	public boolean rejectIt()
	{
		log.info(toString());
		setIsApproved(false);
		return true;
	}	//	rejectIt
	
	/**
	 * 	Complete Document
	 * 	@return new status (Complete, In Progress, Invalid, Waiting ..)
	 */
	public String completeIt()
	{
		//	Re-Check
		if (!m_justPrepared)
		{
			String status = prepareIt();
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		//	Outstanding (not processed) Incoming Confirmations ?
		MMovementConfirm[] confirmations = getConfirmations(true);
		for (int i = 0; i < confirmations.length; i++)
		{
			MMovementConfirm confirm = confirmations[i];
			if (!confirm.isProcessed())
			{
				m_processMsg = "Open: @M_MovementConfirm_ID@ - " 
					+ confirm.getDocumentNo();
				return DocAction.STATUS_InProgress;
			}
		}
		
		//	Implicit Approval
		if (!isApproved())
			approveIt();
		log.info(toString());
		
		//
		MMovementLine[] lines = getLines(false);
		for (int i = 0; i < lines.length; i++)
		{
			MMovementLine line = lines[i];
			MTransaction trxFrom = null; 
//20111226913 if the movement approved with out updating the approved qty then requested qty will become the approved qty			
			if( line.getMovementQty() != null && line.getMovementQty().compareTo( Env.ZERO) == 0 ){
				line.setMovementQty( line.getTargetQty() );
				line.saveEx();
			}
			//Stock Movement - Counterpart MOrder.reserveStock
			MProduct product = line.getProduct();
			if (product != null 
					&& product.isStocked() )
			{
				//Ignore the Material Policy when is Reverse Correction
				if(!isReversal())
					checkMaterialPolicy(line);

				if (line.getM_AttributeSetInstance_ID() == 0)
				{
					MMovementLineMA mas[] = MMovementLineMA.get(getCtx(),
							line.getM_MovementLine_ID(), get_TrxName());
					for (int j = 0; j < mas.length; j++)
					{
						MMovementLineMA ma = mas[j];
						//
						MLocator locator = new MLocator (getCtx(), line.getM_Locator_ID(), get_TrxName());
						//Update Storage 
						if (!MStorage.add(getCtx(),locator.getM_Warehouse_ID(),
								line.getM_Locator_ID(),
								line.getM_Product_ID(), 
								ma.getM_AttributeSetInstance_ID(), 0, 
								ma.getMovementQty().negate(), Env.ZERO ,  Env.ZERO , get_TrxName()))
						{
							m_processMsg = "Cannot correct Inventory (MA)";
							return DocAction.STATUS_Invalid;
						}

						int M_AttributeSetInstanceTo_ID = line.getM_AttributeSetInstanceTo_ID();
						//only can be same asi if locator is different
						if (M_AttributeSetInstanceTo_ID == 0 && line.getM_Locator_ID() != line.getM_LocatorTo_ID())
						{
							M_AttributeSetInstanceTo_ID = ma.getM_AttributeSetInstance_ID();
						}
						//Update Storage 
						if (!MStorage.add(getCtx(),locator.getM_Warehouse_ID(),
								line.getM_LocatorTo_ID(),
								line.getM_Product_ID(), 
								M_AttributeSetInstanceTo_ID, 0, 
								ma.getMovementQty(), Env.ZERO ,  Env.ZERO , get_TrxName()))
						{
							m_processMsg = "Cannot correct Inventory (MA)";
							return DocAction.STATUS_Invalid;
						}

						//20111226913 organization for the transaction should be the organization of which the locator's warehouse belongs
						trxFrom = new MTransaction (getCtx(), line.getM_Locator().getM_Warehouse().getAD_Org_ID(), 
								MTransaction.MOVEMENTTYPE_MovementFrom,
								line.getM_Locator_ID(), line.getM_Product_ID(), ma.getM_AttributeSetInstance_ID(),
								ma.getMovementQty().negate(), getMovementDate(), get_TrxName());
						trxFrom.setM_MovementLine_ID(line.getM_MovementLine_ID());
						if (!trxFrom.save())
						{
							m_processMsg = "Transaction From not inserted (MA)";
							return DocAction.STATUS_Invalid;
						}
						//
						MLocator locatorTo = new MLocator (getCtx(), line.getM_LocatorTo_ID(), get_TrxName());
						MTransaction trxTo = new MTransaction (getCtx(), line.getM_LocatorTo().getM_Warehouse().getAD_Org_ID(), 
								MTransaction.MOVEMENTTYPE_MovementTo,
								line.getM_LocatorTo_ID(), line.getM_Product_ID(), M_AttributeSetInstanceTo_ID,
								ma.getMovementQty(), getMovementDate(), get_TrxName());
						trxTo.setAD_Org_ID(locatorTo.getAD_Org_ID());
						trxTo.setM_MovementLine_ID(line.getM_MovementLine_ID());
						if (!trxTo.save())
						{
							m_processMsg = "Transaction To not inserted (MA)";
							return DocAction.STATUS_Invalid;
						}
					}
				}
				//	Fallback - We have ASI
				if (trxFrom == null)
				{
					MLocator locator = new MLocator (getCtx(), line.getM_Locator_ID(), get_TrxName()); 
					//Update Storage 
					if (!MStorage.add(getCtx(),locator.getM_Warehouse_ID(),
							line.getM_Locator_ID(),
							line.getM_Product_ID(), 
							line.getM_AttributeSetInstance_ID(), 0, 
							line.getMovementQty().negate(), Env.ZERO ,  Env.ZERO , get_TrxName()))
					{
						m_processMsg = "Cannot correct Inventory (MA)";
						return DocAction.STATUS_Invalid;
					}

					//Update Storage 
					if (!MStorage.add(getCtx(),locator.getM_Warehouse_ID(),
							line.getM_LocatorTo_ID(),
							line.getM_Product_ID(), 
							line.getM_AttributeSetInstanceTo_ID(), 0, 
							line.getMovementQty(), Env.ZERO ,  Env.ZERO , get_TrxName()))
					{
						m_processMsg = "Cannot correct Inventory (MA)";
						return DocAction.STATUS_Invalid;
					}

					//
					trxFrom = new MTransaction (getCtx(), line.getM_Locator().getM_Warehouse().getAD_Org_ID(), 
							MTransaction.MOVEMENTTYPE_MovementFrom,
							line.getM_Locator_ID(), line.getM_Product_ID(), line.getM_AttributeSetInstance_ID(),
							line.getMovementQty().negate(), getMovementDate(), get_TrxName());
					trxFrom.setM_MovementLine_ID(line.getM_MovementLine_ID());
					if (!trxFrom.save())
					{
						m_processMsg = "Transaction From not inserted";
						return DocAction.STATUS_Invalid;
					}
					//
					MTransaction trxTo = new MTransaction (getCtx(), line.getM_LocatorTo().getM_Warehouse().getAD_Org_ID(), 
							MTransaction.MOVEMENTTYPE_MovementTo,
							line.getM_LocatorTo_ID(), line.getM_Product_ID(), line.getM_AttributeSetInstanceTo_ID(),
							line.getMovementQty(), getMovementDate(), get_TrxName());
					trxTo.setM_MovementLine_ID(line.getM_MovementLine_ID());
					if (!trxTo.save())
					{
						m_processMsg = "Transaction To not inserted";
						return DocAction.STATUS_Invalid;
					} 
				}	//	Fallback
			} // product stock	
		}	//	for all lines
		//	User Validation
		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (valid != null)
		{
			m_processMsg = valid;
			return DocAction.STATUS_Invalid;
		}
		
		// Set the definite document number after completed (if needed)
		setDefiniteDocumentNo();
		setMovementDate( WTCTimeUtil.getSystemCurrentTimestamp() ); 
		//
		setProcessed(true);
		setisReceived( Boolean.TRUE );
		setDocAction(DOCACTION_Close);
		return DocAction.STATUS_Completed;
	}	//	completeIt
	
	/**
	 * 	Set the definite document number after completed
	 */
	private void setDefiniteDocumentNo() {
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		if (dt.isOverwriteDateOnComplete()) {
			setMovementDate(new Timestamp (System.currentTimeMillis()));
		}
		if (dt.isOverwriteSeqOnComplete()) {
			String value = DB.getDocumentNo(getC_DocType_ID(), get_TrxName(), true, this);
			if (value != null)
				setDocumentNo(value);
		}
	}

	/**
	 * 	Check Material Policy
	 * 	Sets line ASI
	 */
	private void checkMaterialPolicy(MMovementLine line)
	{
		MMovementLineMA.deleteMovementLineMA(line.get_ID(), get_TrxName());
		
		boolean needSave = false;

		//	Attribute Set Instance
		if (line.getM_AttributeSetInstance_ID() == 0)
		{
			MProduct product = MProduct.get(getCtx(), line.getM_Product_ID());
			String MMPolicy = product.getMMPolicy();
			MStorage[] storages = MStorage.getWarehouse(getCtx(), 0, line.getM_Product_ID(), 0, 
					null, MClient.MMPOLICY_FiFo.equals(MMPolicy), true, line.getM_Locator_ID(), get_TrxName());

			BigDecimal qtyToDeliver = line.getMovementQty();

			for (MStorage storage: storages)
			{
				if (storage.getQtyOnHand().compareTo(qtyToDeliver) >= 0)
				{
					MMovementLineMA ma = new MMovementLineMA (line, 
							storage.getM_AttributeSetInstance_ID(),
							qtyToDeliver);
					ma.saveEx();		
					qtyToDeliver = Env.ZERO;
					log.fine( ma + ", QtyToDeliver=" + qtyToDeliver);		
				}
				else
				{	
					MMovementLineMA ma = new MMovementLineMA (line, 
								storage.getM_AttributeSetInstance_ID(),
								storage.getQtyOnHand());
					ma.saveEx();	
					qtyToDeliver = qtyToDeliver.subtract(storage.getQtyOnHand());
					log.fine( ma + ", QtyToDeliver=" + qtyToDeliver);		
				}
				if (qtyToDeliver.signum() == 0)
					break;
			}
							
			//	No AttributeSetInstance found for remainder
			if (qtyToDeliver.signum() != 0)
			{
				//deliver using new asi
				MAttributeSetInstance asi = MAttributeSetInstance.create(getCtx(), product, get_TrxName());
				int M_AttributeSetInstance_ID = asi.getM_AttributeSetInstance_ID();
				MMovementLineMA ma = new MMovementLineMA (line, M_AttributeSetInstance_ID , qtyToDeliver);
				ma.saveEx();
				log.fine("##: " + ma);
			}
		}	//	attributeSetInstance
		
		if (needSave)
		{
			line.saveEx();
		}
	}	//	checkMaterialPolicy

	/**
	 * 	Void Document.
	 * 	@return true if success 
	 */
	public boolean voidIt()
	{
		log.info(toString());
		// Before Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_VOID);
		if (m_processMsg != null)
			return false;
		
		if (DOCSTATUS_Closed.equals(getDocStatus())
			|| DOCSTATUS_Reversed.equals(getDocStatus())
			|| DOCSTATUS_Voided.equals(getDocStatus()))
		{
			m_processMsg = "Document Closed: " + getDocStatus();
			return false;
		}

		//	Not Processed
		if (DOCSTATUS_Drafted.equals(getDocStatus())
			|| DOCSTATUS_Invalid.equals(getDocStatus())
			|| DOCSTATUS_InProgress.equals(getDocStatus())
			|| DOCSTATUS_Approved.equals(getDocStatus())
			|| DOCSTATUS_NotApproved.equals(getDocStatus()) )
		{
			//	Set lines to 0
			MMovementLine[] lines = getLines(false);
			for (int i = 0; i < lines.length; i++)
			{
				MMovementLine line = lines[i];
				BigDecimal old = line.getMovementQty();
				if (old.compareTo(Env.ZERO) != 0)
				{
					line.setMovementQty(Env.ZERO);
					line.addDescription("Void (" + old + ")");
					line.saveEx();
				}
			}
		}
		else
		{
			m_processMsg = "@CanNotVoid@";
			return false;
//			return reverseCorrectIt();
		}
		// After Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_VOID);
		if (m_processMsg != null)
			return false;
			
		setProcessed(true);
		setDocAction(DOCACTION_None);
		return true;
	}	//	voidIt
	
	/**
	 * 	Close Document.
	 * 	@return true if success 
	 */
	public boolean closeIt()
	{
		log.info(toString());
		// Before Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_CLOSE);
		if (m_processMsg != null)
			return false;

		// After Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_CLOSE);
		if (m_processMsg != null)
			return false;

		//	Close Not delivered Qty
		setDocAction(DOCACTION_None);
		return true;
	}	//	closeIt
	
	/**
	 * 	Reverse Correction
	 * 	@return false 
	 */
	public boolean reverseCorrectIt()
	{
		log.info(toString());
		// Before reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		if (m_processMsg != null)
			return false;
		
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		if (!MPeriod.isOpen(getCtx(), getMovementDate(), dt.getDocBaseType(), getAD_Org_ID()))
		{
			m_processMsg = "@PeriodClosed@";
			return false;
		}

		//	Deep Copy
		MMovement reversal = new MMovement(getCtx(), 0, get_TrxName());
		copyValues(this, reversal, getAD_Client_ID(), getAD_Org_ID());
		reversal.setDocStatus(DOCSTATUS_Drafted);
		reversal.setDocAction(DOCACTION_Complete);
		reversal.setIsApproved (false);
		reversal.setIsInTransit (false);
		reversal.setPosted(false);
		reversal.setProcessed(false);
		reversal.addDescription("{->" + getDocumentNo() + ")");
		//FR [ 1948157  ]
		reversal.setReversal_ID(getM_Movement_ID());
		reversal.setReversal(true);
		reversal.setDocumentNo(getDocumentNo()+"^"); // arhipac: teo_sarca
		reversal.saveEx();
		this.setReversal_ID(reversal.get_ID()); // FR [ 1948157  ]
		
		//	Reverse Line Qty
		MMovementLine[] oLines = getLines(true);
		for (int i = 0; i < oLines.length; i++)
		{
			MMovementLine oLine = oLines[i];
			MMovementLine rLine = new MMovementLine(getCtx(), 0, get_TrxName());
			copyValues(oLine, rLine, oLine.getAD_Client_ID(), oLine.getAD_Org_ID());
			rLine.setM_Movement_ID(reversal.getM_Movement_ID());
			//AZ Goodwill			
			// store original (voided/reversed) document line
			rLine.setReversalLine_ID(oLine.getM_MovementLine_ID());
			//
			rLine.setMovementQty(rLine.getMovementQty().negate());
			rLine.setTargetQty(Env.ZERO);
			rLine.setScrappedQty(Env.ZERO);
			rLine.setConfirmedQty(Env.ZERO);
			rLine.setProcessed(false);
			rLine.saveEx();
		}
		//
		if (!reversal.processIt(DocAction.ACTION_Complete))
		{
			m_processMsg = "Reversal ERROR: " + reversal.getProcessMsg();
			return false;
		}
		reversal.closeIt();
		reversal.setDocStatus(DOCSTATUS_Reversed);
		reversal.setDocAction(DOCACTION_None);
		reversal.saveEx();
		m_processMsg = reversal.getDocumentNo();
		
		// After reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSECORRECT);
		if (m_processMsg != null)
			return false;
		
		//	Update Reversed (this)
		addDescription("(" + reversal.getDocumentNo() + "<-)");
		//FR [ 1948157  ]
		setReversal_ID(reversal.getM_Movement_ID());
		setProcessed(true);
		setDocStatus(DOCSTATUS_Reversed);	//	may come from void
		setDocAction(DOCACTION_None);
		
		return true;
	}	//	reverseCorrectionIt
	
	/**
	 * 	Reverse Accrual - none
	 * 	@return false 
	 */
	public boolean reverseAccrualIt()
	{
		log.info(toString());
		// Before reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;
		
		// After reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;
		
		return false;
	}	//	reverseAccrualIt
	
	/** 
	 * 	Re-activate
	 * 	@return false 
	 */
	public boolean reActivateIt()
	{
		log.info(toString());
		// Before reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REACTIVATE);
		if (m_processMsg != null)
			return false;	
		
		// After reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REACTIVATE);
		if (m_processMsg != null)
			return false;
		
		return false;
	}	//	reActivateIt
	
	
	/*************************************************************************
	 * 	Get Summary
	 *	@return Summary of Document
	 */
	public String getSummary()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(getDocumentNo());
		//	: Total Lines = 123.00 (#1)
		sb.append(": ")
			.append(Msg.translate(getCtx(),"ApprovalAmt")).append("=").append(getApprovalAmt())
			.append(" (#").append(getLines(false).length).append(")");
		//	 - Description
		if (getDescription() != null && getDescription().length() > 0)
			sb.append(" - ").append(getDescription());
		return sb.toString();
	}	//	getSummary
	
	/**
	 * 	Get Process Message
	 *	@return clear text error message
	 */
	public String getProcessMsg()
	{
		return m_processMsg;
	}	//	getProcessMsg
	
	/**
	 * 	Get Document Owner (Responsible)
	 *	@return AD_User_ID
	 */
	public int getDoc_User_ID()
	{
		return getCreatedBy();
	}	//	getDoc_User_ID

	/**
	 * 	Get Document Currency
	 *	@return C_Currency_ID
	 */
	public int getC_Currency_ID()
	{
	//	MPriceList pl = MPriceList.get(getCtx(), getM_PriceList_ID());
	//	return pl.getC_Currency_ID();
		return 0;
	}	//	getC_Currency_ID
	
	/** Reversal Flag		*/
	private boolean m_reversal = false;
	
	/**
	 * 	Set Reversal
	 *	@param reversal reversal
	 */
	private void setReversal(boolean reversal)
	{
		m_reversal = reversal;
	}	//	setReversal
	/**
	 * 	Is Reversal
	 *	@return reversal
	 */
	private boolean isReversal()
	{
		return m_reversal;
	}	//	isReversal

	/**
	 * 	Document Status is Complete or Closed
	 *	@return true if CO, CL or RE
	 */
	public boolean isComplete()
	{
		String ds = getDocStatus();
		return DOCSTATUS_Completed.equals(ds) 
			|| DOCSTATUS_Closed.equals(ds)
			|| DOCSTATUS_Reversed.equals(ds);
	}	//	isComplete
/*	// CostDetail
	private void createCostDetail(MTransaction trxFrom, MTransaction trxTo, MMovementLine line)
	{
		if (trxFrom.getMovementQty().compareTo(trxTo.getMovementQty().negate()) != 0)
		{
			throw new AdempiereException("Same qty is needed - "+trxFrom+", "+trxTo);
		}
		//
		for(MAcctSchema as : MAcctSchema.getClientAcctSchema(getCtx(), trxFrom.getAD_Client_ID()))
		{
			ProductCost pc = new ProductCost (getCtx(), 
					trxFrom.getM_Product_ID(), trxFrom.getM_AttributeSetInstance_ID(),
					get_TrxName());
			pc.setQty(trxFrom.getMovementQty());
			List<CostComponent> costs = pc.getProductCostsLayers(as, line.getAD_Org_ID(),
								null, // CostingMethod
								line.get_ID(),
								false); // zeroCostsOK
			String description = line.getDescription();
			if (description == null)
				description = "";

			for (CostComponent cc : costs)
			{
				final boolean sameCostDimension = isSameCostDimension(as, trxFrom, trxTo);
				MCostDetail.createMovement(as,
						trxFrom.getAD_Org_ID(), 	//	locator org
						line.getM_Product_ID(), trxFrom.getM_AttributeSetInstance_ID(),
						line.get_ID(),
						0, // No Cost Element 
						cc.getAmount(), cc.qty,
						true, // IsSOTrx
						sameCostDimension, // Is Autoprocess
						description + "(|->)",
						get_TrxName());
				//
				MCostDetail.createMovement(as,
						trxTo.getAD_Org_ID(),	//	locator org 
						line.getM_Product_ID(), trxTo.getM_AttributeSetInstance_ID(),
						line.get_ID(),
						0, // No CostElement 
						cc.getAmount().negate(), cc.qty.negate(),
						false, // IsSOTrx
						sameCostDimension, // Is Autoprocess
						description + "(|<-)",
						get_TrxName());
			}
		}
	}*/
	
	private boolean isSameCostDimension(MAcctSchema as, MTransaction trxFrom, MTransaction trxTo)
	{
		if (trxFrom.getM_Product_ID() != trxTo.getM_Product_ID())
		{
			throw new AdempiereException("Same product is needed - "+trxFrom+", "+trxTo);
		}
		MProduct product = MProduct.get(getCtx(), trxFrom.getM_Product_ID());
		String CostingLevel = product.getCostingLevel(as,trxFrom.getAD_Org_ID());
		int Org_ID = trxFrom.getAD_Org_ID();
		int Org_ID_To = trxTo.getAD_Org_ID();
		int ASI_ID = trxFrom.getM_AttributeSetInstance_ID();
		int ASI_ID_To = trxTo.getM_AttributeSetInstance_ID();
		if (MAcctSchema.COSTINGLEVEL_Client.equals(CostingLevel))
		{
			Org_ID = 0;
			Org_ID_To = 0;
			ASI_ID = 0;
			ASI_ID_To = 0;
		}
		else if (MAcctSchema.COSTINGLEVEL_Organization.equals(CostingLevel))
		{
			ASI_ID = 0;
			ASI_ID_To = 0;
		}
		else if (MAcctSchema.COSTINGLEVEL_BatchLot.equals(CostingLevel))
		{
			Org_ID = 0;
			Org_ID_To = 0;
		}
		//
		return Org_ID == Org_ID_To && ASI_ID == ASI_ID_To;
	}

	public int getDynamicUserID() {
		
		Integer warehouseID = getM_Warehouse_ID();
		String sql = "SELECT AD_User_ID FROM AD_User WHERE C_BPartner_ID = (SELECT C_BPartner_ID FROM M_Warehouse WHERE M_Warehouse_ID= ?)";
		Integer representativeID  = DB.getSQLValue(get_TrxName(), sql, warehouseID);
		
		if(representativeID==null)
				return 0;
		return representativeID.intValue();
	}

	public int getDynamicRoleID() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Timestamp getHideTime() {
		// TODO Auto-generated method stub
		return null;
	}
}	//	MMovement
