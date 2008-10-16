package org.mobicents.servlet.management.client.router;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.data.ArrayReader;
import com.gwtext.client.data.FieldDef;
import com.gwtext.client.data.MemoryProxy;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.SimpleStore;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.form.ComboBox;
import com.gwtext.client.widgets.form.FormPanel;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.form.event.ComboBoxListenerAdapter;

public class ApplicationRouteNodeEditor extends VerticalPanel {
	final FormPanel formPanel = new FormPanel();
	private Widget dragHandle;
	private ComboBox applicationName;
	private TextField subscriberIdentity;
	private ComboBox routingRegion; // TERMINATING ORIGINATING
	private TextField route;
	private ComboBox routeModified; //CLEAR NO_ROUTE ROUTE
	private TextField order;
	private boolean isNew = true; // if this node is just created by the user or loaded from the config file

	private static Object[][] regions = new Object[][]{  
		new Object[]{"TERMINATING"},  
		new Object[]{"ORIGINATING"},
		new Object[]{"NEUTRAL"}
	};  

	private static Object[][] routeModifiers = new Object[][]{  
		new Object[]{"NO_ROUTE"},  
		new Object[]{"ROUTE"},
		new Object[]{"CLEAR_ROUTE"}
	};  

	private void addLabeledControl(String label, Widget component, Panel panel) {
		Panel regionLabel = new Panel();
		regionLabel.setPaddings(0, 0, 0, 1);
		regionLabel.setBorder(false);
		regionLabel.setHtml(label);
		panel.add(regionLabel);
		panel.add(component);
	}
	
	private ComboBox makeCombo(Store store, String field, ComboBoxListenerAdapter listener, String defaultValue) {
		final ComboBox box;
		box = new ComboBox();  
		box.setForceSelection(true);  
		box.setStore(store);  
		box.setDisplayField(field);  
		box.setMode(ComboBox.LOCAL);  
		box.setTriggerAction(ComboBox.ALL);  
		box.setEmptyText("Select Value");  
		box.setValueField(field);
		box.setSelectOnFocus(true);  
		box.setEditable(true);
		box.setHideLabel(true);
		box.setWidth(160); 
		box.setHideTrigger(false);
		box.addListener(listener);
		box.setValue(defaultValue);
		return box;
	}
	
	private void loadApplications() {
		applicationName = new ComboBox();  
		// applicationName.setForceSelection(true);  
 
		applicationName.setMode(ComboBox.LOCAL);  
		applicationName.setTriggerAction(ComboBox.ALL);  
		applicationName.setEmptyText("...");  
		
		applicationName.setEditable(true);
		applicationName.setHideLabel(true);
		applicationName.setWidth(188); 
		applicationName.setHideTrigger(false);
		final Store appsStore = new SimpleStore(new String[]{"apps"}, new Object[][]{{}});  
		appsStore.load();  
		applicationName.setStore(appsStore);  
		applicationName.setDisplayField("apps");
		applicationName.setValueField("apps");
		
		DARConfigurationService.Util.getInstance().getApplications(new AsyncCallback(){

			public void onFailure(Throwable arg0) {
				Console.error("Could not enumerate deployed applications.");
				
			}

			public void onSuccess(Object appsObj) {
				
				String[] apps = (String[]) appsObj;
				
				Object[][] appsStoreArray = new Object[apps.length][1];
				for(int q=0; q< apps.length; q++) {
					appsStoreArray[q][0] = apps[q];
				}
				
				// This hack is needed because of http://www.gwt-ext.com/forum/viewtopic.php?f=5&t=113
				MemoryProxy proxy = new MemoryProxy(appsStoreArray);
				ArrayReader reader = new ArrayReader(new RecordDef(
						   new FieldDef[]{
						     new StringFieldDef("apps")
						     }
						  ));
				Store storeTemp = new Store(proxy, reader);
				storeTemp.reload();
				Store original = applicationName.getStore();
				original.removeAll();
				original.add(storeTemp.getRecords());
				if(apps.length>0 && isNew) applicationName.setValue(apps[0]);
				Console.info("Successfully enumerated deployed applications");
			}
			
		});
		
	}
	
	private TextField makeTextField(String label, String name, int length) {
		TextField text = new TextField(label, name, length);  
		text.setAllowBlank(true); 
		text.setHideLabel(true);
		return text;
	}
	
	public ApplicationRouteNodeEditor() {
		dragHandle = new HTML("<div class='drag-handle'/>");

		// App name
		loadApplications();

		// Subscriber id 
		this.subscriberIdentity = makeTextField("Subscriber", "Subscriber", 160);

		// Routing Region
		final Store regionStore = new SimpleStore(new String[]{"region"}, regions);  
		regionStore.load();  
	
		this.routingRegion = makeCombo(regionStore, "region", new RoutingRegionComboListener(this), (String) regions[0][0]);  
		
		// Route 
		this.route = makeTextField("Route", "Route", 160);

		// Route modifiers
		final Store modifiersStore = new SimpleStore(new String[]{"modifiers"}, routeModifiers);  
		regionStore.load();  
	
		this.routeModified = makeCombo(modifiersStore, "modifiers", new ComboBoxListenerAdapter() {  

			public void onSelect(ComboBox comboBox, com.gwtext.client.data.Record record, int index) {  
				System.out.println("Route modifiers::onSelect('" + record.getAsString("modifiers") + "')");  
			}  
		}, (String) routeModifiers[0][0]);  
		
		// Order 
		this.order = makeTextField("Order", "Order", 160);
		this.order.setBlankText("0");
		
		// Set up the form
		formPanel.setFrame(true);   
		formPanel.setWidth(200);  
		formPanel.setLabelWidth(75);   
		
		final Panel collapsedPanel = new Panel(); // collapsedPanel doesn't work. TODO: FIXME
		addLabeledControl("Application Name", applicationName, formPanel);
		addLabeledControl("Subscriber Identity", subscriberIdentity, collapsedPanel);
		addLabeledControl("Routing region", routingRegion, collapsedPanel);
		addLabeledControl("Route", route, collapsedPanel);
		addLabeledControl("Route modifiers", routeModified, collapsedPanel);
		addLabeledControl("Order", order, collapsedPanel);
		collapsedPanel.setCollapsible(true);
		collapsedPanel.setTitle("Options");
		collapsedPanel.setCollapsed(true);
		collapsedPanel.addStyleName("ssm-collapsed-background");
		formPanel.add(collapsedPanel);

		// Add delete button to remove this item from the app list
		Button deleteButton = new Button("Delete");
		final ApplicationRouteNodeEditor finalThis = this;
		
		deleteButton.addListener(new ButtonListenerAdapter() {
			public void onClick(Button button, EventObject e) {
				final VerticalPanel parent = (VerticalPanel) finalThis.getParent();
				parent.remove((Widget)finalThis);
			}
		});
		//deleteButton.setWidth("188px");
		Panel space = new Panel();
		space.setWidth(100);
		space.setHeight(15);
		formPanel.add(space);
		formPanel.add(deleteButton);

		// Set up the top level panel
		add(dragHandle);
		add(formPanel);
	}
	
	public ApplicationRouteNodeEditor(DARRouteNode node) {
		this();
		isNew = false;
		setApplicationName(node.getApplication());
		setSubscriberIdentity(node.getSubscriber());
		setOrder(node.getOrder());
		setRoute(node.getRoute());
		setRouteModified(node.getRouteModifier());
		setRoutingRegion(node.getRoutingRegion());
		
	}

	public Widget getDragHandle() {
		return dragHandle;

	}

	public String getApplicationName() {
		return applicationName.getText();
	}
	
	public void setApplicationName(String str) {
		this.applicationName.setValue(str);
	}

	public String getSubscriberIdentity() {
		return subscriberIdentity.getText();
	}
	public void setSubscriberIdentity(String str) {
		this.subscriberIdentity.setValue(str);
	}

	public String getRoutingRegion() {
		return routingRegion.getValue();
	}
	
	public void setRoutingRegion(String str) {
		this.routingRegion.setValue(str);
	}

	public String getRoute() {
		return route.getText();
	}
	
	public void setRoute(String str) {
		this.route.setValue(str);
	}
	
	public String getRouteModified() {
		return routeModified.getValue();
	}
	
	public void setRouteModified(String str) {
		this.routeModified.setValue(str);
	}
	
	public String getOrder() {
		String text = order.getText();
		if(text == null || text.equals("")) return "0";
		return order.getText();
	}
	
	public void setOrder(String str) {
		this.order.setValue(str);
	}
	
	public String toString() {
		// INVITE: ("org.mobicents.servlet.sip.example.SimpleSipServlet", "DAR:From", "ORIGINATING", "", "NO_ROUTE", "0")
		String result = "(" + quoteString(getApplicationName()) + ", " + quoteString(getSubscriberIdentity()) + ", " +
		quoteString(getRoutingRegion()) + ", " + quoteString(getRoute()) + ", " +
		quoteString(getRouteModified()) + ", " + quoteString(getOrder()) + ")";
		return result;
	}
	private static String quoteString(String str) {
		if( str == null ) str = "";
		return "\"" + str + "\"";
	}
}
