package org.adempiere.pos;


import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.compiere.model.MImage;
import org.compiere.model.MPOSKey;
import org.compiere.model.MProduct;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zkex.zul.North;

public class WPOSInfoProduct extends WPosSubPanel {

	private Panel parameterPanel;
	/**	Image Product		*/
	private Button		bImage;
	/**	Product Code		*/
	private Label 		fValue;
	/**	Product Name		*/
	private Label 		fName;
	/**	Product Price		*/
	private Label 		fPrice;
	/**	Product Description	*/
	private Label		fDescription;
	
	public WPOSInfoProduct(WPOS posPanel) {
		super(posPanel);
		
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -175459707049618428L;

	@Override
	public void onEvent(Event arg0) throws Exception {
		
	}

	@Override
	protected void init() {
		parameterPanel = new Panel();
		Grid infoProductLayout = GridFactory.newGridLayout();
		Grid labelLayout = GridFactory.newGridLayout();
		Borderlayout fullPanel = new Borderlayout();
		
		Rows rows = null;
		Row row = null;	
		North north = new North();

		north.setStyle("border: none; width:100%");
		north.setZindex(0);
		fullPanel.appendChild(north);

		Panel buttonPanel = new Panel();

		buttonPanel.appendChild(labelLayout);
		parameterPanel.appendChild(infoProductLayout);
		infoProductLayout.setWidth("100%");
		infoProductLayout.setHeight("100%");
		rows = infoProductLayout.newRows();
		row = rows.newRow();
		
		//	For Image
		bImage = new Button("-");
		row.appendChild(bImage);
			
		
		row.appendChild(buttonPanel);
		rows = labelLayout.newRows();
		row = rows.newRow();
		//	For Value
		fValue = new Label (Msg.getElement(Env.getCtx(), "ProductValue"));
		fValue.setStyle("Font-size:medium; font-weight:bold");
		//	Add
		row.appendChild(fValue);
		fPrice = new Label (Msg.getElement(Env.getCtx(), "Price"));
		fPrice.setStyle("Font-size:medium; font-weight:bold");
		//	Add
		row.appendChild(fPrice);
		
		row = rows.newRow();
		//	For Name
		fName = new Label (Msg.getElement(Env.getCtx(), "ProductName"));
		fName.setStyle("Font-size:medium; font-weight:bold");
		//	Add
		row.appendChild(fName);
		
		row = rows.newRow();
		//	For Description
		fDescription = new Label (Msg.getElement(Env.getCtx(), "Description"));

		fDescription.setClass("label-description");
		//	Add
		row.appendChild(fDescription);

	}
	
	public Panel getPanel(){
		return parameterPanel;
	}
	/**
	 * Refresh Product from Key
	 * @param key
	 * @return void
	 */
	public void refreshProduct(MPOSKey key) {
		if(key == null)
			return;
		//	Refresh Values
		MProduct m_Product = MProduct.get(m_ctx, key.getM_Product_ID());
		if(m_Product != null) {
			String currencyISO_Code = v_POSPanel.getCurSymbol();
			fValue.setText(m_Product.getValue());
			fPrice.setText(currencyISO_Code + " " 
						+ v_POSPanel.getNumberFormat()
							.format(v_POSPanel.getPrice(m_Product)));
			fName.setText(m_Product.getName());
			fDescription.setText(m_Product.getDescription());
		}
		if(key.getAD_Image_ID() != 0) {
			MImage image = MImage.get(Env.getCtx(), key.getAD_Image_ID());
			AImage img = null;
			byte[] data = image.getData();
			if (data != null && data.length > 0) {
				try {
					img = new AImage(null, data);				
				} catch (Exception e) {		
				}
			}
			bImage.setWidth("50px");
			bImage.setHeight("50px");
			
			//	Change Image Size
//			bImage.setImageContent(img);
		} else {
//			bImage.setImageContent(null);
		}
	}
	
	/**
	 * Refresh from product
	 * @param p_M_Product_ID
	 * @return void
	 */
	public void refreshProduct(int p_M_Product_ID) {
		//	Valid Product
		if(p_M_Product_ID == 0)
			return;
		MProduct m_Product = MProduct.get(m_ctx, p_M_Product_ID);
		//	Refresh Product
		if(m_Product != null) {
			String currencyISO_Code = v_POSPanel.getCurSymbol();
			fValue.setText(m_Product.getValue());
			fPrice.setText(currencyISO_Code + " " 
						+ v_POSPanel.getNumberFormat()
							.format(v_POSPanel.getPrice(m_Product)));
			fName.setText(m_Product.getName());
			fDescription.setText(m_Product.getDescription());
		}
		//	Get POS Key
		int m_C_POSKey_ID = DB.getSQLValue(null, "SELECT pk.C_POSKey_ID "
				+ "FROM C_POSKey pk "
				+ "WHERE pk.C_POSKeyLayout_ID = ? "
				+ "AND pk.M_Product_ID = ? "
				+ "AND pk.IsActive = 'Y'", v_POSPanel.getC_POSKeyLayout_ID(), p_M_Product_ID);
		//	Valid POS Key
		if(m_C_POSKey_ID <= 0) {
			return;
		}
		MPOSKey key =  new MPOSKey(m_ctx, m_C_POSKey_ID, null);
		
		if(key.getAD_Image_ID() != 0) {
			MImage image = MImage.get(Env.getCtx(), key.getAD_Image_ID());
			AImage img = null;
			byte[] data = image.getData();
			if (data != null && data.length > 0) {
				try {
					img = new AImage(null, data);				
				} catch (Exception e) {		
				}
			}
			
			//	Change Image Size
//			bImage.setImageContent(img);
		} else {
//			bImage.setImageContent(null);
		}
	}
}
