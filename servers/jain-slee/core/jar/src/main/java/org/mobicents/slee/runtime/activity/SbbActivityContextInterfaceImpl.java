package org.mobicents.slee.runtime.activity;

import java.util.Map;

import javax.slee.SLEEException;
import javax.slee.SbbLocalObject;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.TransactionRolledbackLocalException;

import org.mobicents.slee.container.component.SbbComponent;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.sbb.MActivityContextAttributeAlias;

/**
 * Base class code for a concrete implementation of {@link javax.slee.ActivityContextInterface} by an sbb.
 * @author martins
 *
 */
public class SbbActivityContextInterfaceImpl implements ActivityContextInterface {

	private final ActivityContextInterfaceImpl aciImpl;
	private final Map<String,MActivityContextAttributeAlias> aliases;
	private final String nonAliasedFieldNamePrefix;
	
	public SbbActivityContextInterfaceImpl(ActivityContextInterfaceImpl aciImpl, SbbComponent sbbComponent) {
		this.aciImpl = aciImpl;
		this.aliases = sbbComponent.getDescriptor().getActivityContextAttributeAliases();
		this.nonAliasedFieldNamePrefix = sbbComponent.getSbbID().toString() + ".";
	}

	public ActivityContext getActivityContext() {
		return aciImpl.getActivityContext();
	}

	public ActivityContextInterfaceImpl getAciImpl() {
		return aciImpl;
	}
	
	@Override
    public int hashCode() {    	
    	return aciImpl.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
    	if (obj != null && obj instanceof SbbActivityContextInterfaceImpl) {
    		return ((SbbActivityContextInterfaceImpl)obj).aciImpl.equals(this.aciImpl);
    	}
    	else {
    		return false;
    	}
    }

	public void attach(SbbLocalObject arg0) throws NullPointerException,
			TransactionRequiredLocalException,
			TransactionRolledbackLocalException, SLEEException {
		aciImpl.attach(arg0);	
	}

	public void detach(SbbLocalObject arg0) throws NullPointerException,
			TransactionRequiredLocalException,
			TransactionRolledbackLocalException, SLEEException {
		aciImpl.detach(arg0);
	}

	public Object getActivity() throws TransactionRequiredLocalException,
			SLEEException {		
		return aciImpl.getActivity();
	}

	public boolean isEnding() throws TransactionRequiredLocalException,
			SLEEException {
		return aciImpl.isEnding();
	}	
	
	public boolean isAttached(SbbLocalObject arg0) throws NullPointerException,
			TransactionRequiredLocalException,
			TransactionRolledbackLocalException, SLEEException {
		return aciImpl.isAttached(arg0);
	}
	
	/**
	 * Computes the real aci data field name 
	 * @param fieldName
	 * @return
	 */
	private String getRealFieldName(String fieldName) {
		
		MActivityContextAttributeAlias alias = this.aliases.get(fieldName);
		if (alias != null) {
			return alias.getAttributeAliasName();
		}
		else {
			return nonAliasedFieldNamePrefix + fieldName;
		}
	}
	
	/**
	 * Sets an sbb aci data field value
	 * @param fieldName
	 * @param value
	 */
	public void setFieldValue(String fieldName, Object value) {
		String realFieldName = getRealFieldName(fieldName);
		aciImpl.getActivityContext().setDataAttribute(realFieldName,value);
	}
	
	/**
	 * Sets an sbb aci data field value
	 * @param fieldName
	 * @param value
	 */
	public void setFieldValue(String fieldName, byte value) {
		String realFieldName = getRealFieldName(fieldName);
		aciImpl.getActivityContext().setDataAttribute(realFieldName,value);
	}
	/**
	 * Sets an sbb aci data field value
	 * @param fieldName
	 * @param value
	 */
	public void setFieldValue(String fieldName, short value) {
		String realFieldName = getRealFieldName(fieldName);
		aciImpl.getActivityContext().setDataAttribute(realFieldName,value);
	}
	/**
	 * Sets an sbb aci data field value
	 * @param fieldName
	 * @param value
	 */
	public void setFieldValue(String fieldName, int value) {
		String realFieldName = getRealFieldName(fieldName);
		aciImpl.getActivityContext().setDataAttribute(realFieldName,value);
	}
	/**
	 * Sets an sbb aci data field value
	 * @param fieldName
	 * @param value
	 */
	public void setFieldValue(String fieldName, long value) {
		String realFieldName = getRealFieldName(fieldName);
		aciImpl.getActivityContext().setDataAttribute(realFieldName,value);
	}
	/**
	 * Sets an sbb aci data field value
	 * @param fieldName
	 * @param value
	 */
	public void setFieldValue(String fieldName, float value) {
		String realFieldName = getRealFieldName(fieldName);
		aciImpl.getActivityContext().setDataAttribute(realFieldName,value);
	}
	/**
	 * Sets an sbb aci data field value
	 * @param fieldName
	 * @param value
	 */
	public void setFieldValue(String fieldName, double value) {
		String realFieldName = getRealFieldName(fieldName);
		aciImpl.getActivityContext().setDataAttribute(realFieldName,value);
	}
	/**
	 * Sets an sbb aci data field value
	 * @param fieldName
	 * @param value
	 */
	public void setFieldValue(String fieldName, boolean value) {
		String realFieldName = getRealFieldName(fieldName);
		aciImpl.getActivityContext().setDataAttribute(realFieldName,value);
	}
	/**
	 * Sets an sbb aci data field value
	 * @param fieldName
	 * @param value
	 */
	public void setFieldValue(String fieldName, char value) {
		String realFieldName = getRealFieldName(fieldName);
		aciImpl.getActivityContext().setDataAttribute(realFieldName,value);
	}
	
	/**
	 * Retrieves an sbb aci data field value
	 * @param fieldName
	 * @param returnType
	 * @return
	 */
	public Object getFieldValue(String fieldName, Class returnType) {
	
		String realFieldName = getRealFieldName(fieldName);
		
		Object value = aciImpl.getActivityContext().getDataAttribute(realFieldName);
       
        if ( value == null ) {
            if ( returnType.isPrimitive()) {
                if ( returnType.equals(Integer.TYPE)) {
                    return Integer.valueOf(0);
                } else if ( returnType.equals(Boolean.TYPE)) {
                    return Boolean.FALSE;
                } else if ( returnType.equals( Long.TYPE ) ) {
                    return Long.valueOf(0);
                } else if ( returnType.equals ( Double.TYPE )) {
                    return Double.valueOf(0);
                } else if ( returnType.equals(Float.TYPE)) {
                    return Float.valueOf(0);
                }
            }
        }
       
        return value; 
	}
}
