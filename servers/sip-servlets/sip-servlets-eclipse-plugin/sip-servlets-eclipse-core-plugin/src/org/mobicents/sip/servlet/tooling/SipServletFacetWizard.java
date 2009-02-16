package org.mobicents.sip.servlet.tooling;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.common.project.facet.ui.AbstractFacetWizardPage;

public final class SipServletFacetWizard extends AbstractFacetWizardPage
{
    private SipServletApplicationConfig config;
	private Text appName;
	private Text description;
	private Text mainServletName;
	private Text mainServletDesciption;
	private Text mainServletClass;

    public SipServletFacetWizard()
    {
        super( "mobicents.servlet.sip.facet.install.wizard.app" );

        setTitle( "SIP Application" );
        setDescription( "Configure the SIP Application and the main servlet for the application." );
    }
    
    private String capitalizeFirstLetter(String string) {
    	if(string.length()<1) return "";
    	return string.substring(0, 1).toUpperCase() + string.substring(1, string.length());
    }

    public void createControl( final Composite parent )
    {
    	String projectName = context.getProjectName();
    	if(projectName == null) projectName = "";
    	String fixedProjectName = capitalizeFirstLetter(
    			projectName.replace("-", "").replace(".", ""));
        final Composite composite = new Composite( parent, SWT.NONE );
        composite.setLayout( new GridLayout( 1, false ) );

        Label label = new Label( composite, SWT.NONE );
        label.setLayoutData( gdhfill() );
        label.setText( "SIP Application Name:" );

        this.appName = new Text( composite, SWT.BORDER );
        this.appName.setLayoutData( gdhfill() );
        this.appName.setText( projectName );
        
        label = new Label( composite, SWT.NONE );
        label.setLayoutData( gdhfill() );
        label.setText( "SIP Application Description:" );
        
        this.description = new Text( composite, SWT.BORDER );
        this.description.setLayoutData( gdhfill() );
        this.description.setText( projectName );
        
        label = new Label( composite, SWT.NONE );
        label.setLayoutData( gdhfill() );
        label.setText( "Main Servlet Name:" );
        
        this.mainServletName = new Text( composite, SWT.BORDER );
        this.mainServletName.setLayoutData( gdhfill() );
        this.mainServletName.setText(fixedProjectName );
        
        label = new Label( composite, SWT.NONE );
        label.setLayoutData( gdhfill() );
        label.setText( "Main Servlet Description:" );
        
        this.mainServletDesciption = new Text( composite, SWT.BORDER );
        this.mainServletDesciption.setLayoutData( gdhfill() );
        this.mainServletDesciption.setText(fixedProjectName);
        
        label = new Label( composite, SWT.NONE );
        label.setLayoutData( gdhfill() );
        label.setText( "Main Servlet Class:" );
        
        this.mainServletClass = new Text( composite, SWT.BORDER );
        this.mainServletClass.setLayoutData( gdhfill() );
        this.mainServletClass.setText("org.example.servlet.sip." + fixedProjectName);

        setControl( composite );
    }

    public void setConfig( final Object config )
    {
        this.config = (SipServletApplicationConfig) config;
    }

    public void transferStateToConfig()
    {
        this.config.setAppName(this.appName.getText());
        this.config.setDescription(this.description.getText());
        this.config.setMainServletClass(this.mainServletClass.getText());
        this.config.setMainServletDesciption(this.mainServletDesciption.getText());
        this.config.setMainServletName(this.mainServletName.getText());
    }

    private static GridData gdhfill()
    {
        return new GridData( GridData.FILL_HORIZONTAL );
    }
}
