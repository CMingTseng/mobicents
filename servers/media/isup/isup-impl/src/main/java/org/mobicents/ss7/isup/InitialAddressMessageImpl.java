/**
 * Start time:08:17:13 2009-07-17<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
 * 
 */
package org.mobicents.ss7.isup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.mobicents.ss7.isup.message.parameter.*;
import org.mobicents.ss7.isup.message.parameter.accessTransport.*;
import org.mobicents.ss7.isup.message.InitialAddressMessage;

/**
 * Start time:08:17:13 2009-07-17<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
 */
public class InitialAddressMessageImpl extends ISUPMessageImpl implements InitialAddressMessage {

	public static final MessageTypeImpl _MESSAGE_TYPE = new MessageTypeImpl(_MESSAGE_CODE_IAM);
	private static final int _MANDATORY_VAR_COUNT = 1;
	// mandatory fixed L
	protected static final int _INDEX_F_MessageType = 0;
	protected static final int _INDEX_F_NatureOfConnectionIndicators = 1;
	protected static final int _INDEX_F_ForwardCallIndicators = 2;
	protected static final int _INDEX_F_CallingPartyCategory = 3;
	protected static final int _INDEX_F_TransmissionMediumRequirement = 4;
	// mandatory variable L
	protected static final int _INDEX_V_CalledPartyNumber = 0;
	// optional
	protected static final int _INDEX_O_TransitNetworkSelection = 0;
	protected static final int _INDEX_O_CallReference = 1;
	protected static final int _INDEX_O_CallingPartyNumber = 2;
	protected static final int _INDEX_O_OptionalForwardCallIndicators = 3;
	protected static final int _INDEX_O_RedirectingNumber = 4;
	protected static final int _INDEX_O_RedirectionInformation = 5;
	protected static final int _INDEX_O_ClosedUserGroupInterlockCode = 6;
	protected static final int _INDEX_O_ConnectionRequest = 7;
	protected static final int _INDEX_O_OriginalCalledNumber = 8;
	protected static final int _INDEX_O_UserToUserInformation = 9;
	protected static final int _INDEX_O_AccessTransport = 10;
	protected static final int _INDEX_O_UserServiceInformation = 11;
	protected static final int _INDEX_O_User2UIndicators = 12;
	protected static final int _INDEX_O_GenericNumber = 13;
	protected static final int _INDEX_O_PropagationDelayCounter = 14;
	protected static final int _INDEX_O_UserServiceInformationPrime = 15;
	protected static final int _INDEX_O_NetworkSPecificFacility = 16;
	protected static final int _INDEX_O_GenericDigits = 17;
	protected static final int _INDEX_O_OriginatingISCPointCode = 18;
	protected static final int _INDEX_O_UserTeleserviceInformation = 19;
	protected static final int _INDEX_O_RemoteOperations = 20;
	protected static final int _INDEX_O_ParameterCompatibilityInformation = 21;
	protected static final int _INDEX_O_GenericNotificationIndicator = 22;
	protected static final int _INDEX_O_ServiceActivation = 23;
	protected static final int _INDEX_O_GenericReference = 24;
	protected static final int _INDEX_O_MLPPPrecedence = 25;
	protected static final int _INDEX_O_TransimissionMediumRequierementPrime = 26;
	protected static final int _INDEX_O_LocationNumber = 27;
	protected static final int _INDEX_O_ForwardGVNS = 28;
	protected static final int _INDEX_O_CCSS = 29;
	protected static final int _INDEX_O_NetworkManagementControls = 30;
	protected static final int _INDEX_O_EndOfOptionalParameters = 31;

	protected static final List<Integer> mandatoryParam;
	static {
		List<Integer> tmp = new ArrayList<Integer>();
		tmp.add(_INDEX_F_MessageType);
		tmp.add(_INDEX_F_NatureOfConnectionIndicators);
		tmp.add(_INDEX_F_ForwardCallIndicators);
		tmp.add(_INDEX_F_CallingPartyCategory);
		tmp.add(_INDEX_F_TransmissionMediumRequirement);

		mandatoryParam = Collections.unmodifiableList(tmp);

	}

	InitialAddressMessageImpl(Object source, byte[] b) throws ParameterRangeInvalidException {
		this(source);

		decodeElement(b);

	}

	InitialAddressMessageImpl(Object source) {
		super(source);
		super.f_Parameters = new TreeMap<Integer, ISUPParameter>();
		super.v_Parameters = new TreeMap<Integer, ISUPParameter>();
		super.o_Parameters = new TreeMap<Integer, ISUPParameter>();

		super.f_Parameters.put(_INDEX_F_MessageType, this.getMessageType());

		super.mandatoryCodes.add(NatureOfConnectionIndicators._PARAMETER_CODE);
		super.mandatoryCodes.add(ForwardCallIndicators._PARAMETER_CODE);
		super.mandatoryCodes.add(CallingPartyCategory._PARAMETER_CODE);
		super.mandatoryCodes.add(TransmissionMediumRequirement._PARAMETER_CODE);

		super.mandatoryCodeToIndex.put(NatureOfConnectionIndicators._PARAMETER_CODE, _INDEX_F_NatureOfConnectionIndicators);
		super.mandatoryCodeToIndex.put(ForwardCallIndicators._PARAMETER_CODE, _INDEX_F_NatureOfConnectionIndicators);
		super.mandatoryCodeToIndex.put(CallingPartyCategory._PARAMETER_CODE, _INDEX_F_CallingPartyCategory);
		super.mandatoryCodeToIndex.put(TransmissionMediumRequirement._PARAMETER_CODE, _INDEX_F_TransmissionMediumRequirement);

		super.mandatoryVariableCodes.add(CalledPartyNumber._PARAMETER_CODE);
		super.mandatoryVariableCodeToIndex.put(CalledPartyNumber._PARAMETER_CODE, _INDEX_V_CalledPartyNumber);

		super.optionalCodes.add(TransitNetworkSelection._PARAMETER_CODE);
		super.optionalCodes.add(CallReference._PARAMETER_CODE);
		super.optionalCodes.add(CallingPartyNumber._PARAMETER_CODE);
		super.optionalCodes.add(OptionalForwardCallIndicators._PARAMETER_CODE);
		super.optionalCodes.add(RedirectingNumber._PARAMETER_CODE);
		super.optionalCodes.add(RedirectionInformation._PARAMETER_CODE);
		super.optionalCodes.add(ClosedUserGroupInterlockCode._PARAMETER_CODE);
		super.optionalCodes.add(ConnectionRequest._PARAMETER_CODE);
		super.optionalCodes.add(OriginalCalledNumberImpl._PARAMETER_CODE);
		super.optionalCodes.add(UserToUserInformation._PARAMETER_CODE);
		super.optionalCodes.add(AccessTransport._PARAMETER_CODE);
		super.optionalCodes.add(UserServiceInformation._PARAMETER_CODE);
		super.optionalCodes.add(UserToUserIndicators._PARAMETER_CODE);
		super.optionalCodes.add(GenericNumber._PARAMETER_CODE);
		super.optionalCodes.add(PropagationDelayCounter._PARAMETER_CODE);
		super.optionalCodes.add(UserServiceInformationPrime._PARAMETER_CODE);
		super.optionalCodes.add(NetworkSpecificFacility._PARAMETER_CODE);
		super.optionalCodes.add(GenericDigits._PARAMETER_CODE);
		super.optionalCodes.add(OriginatingISCPointCode._PARAMETER_CODE);
		super.optionalCodes.add(UserTeleserviceInformation._PARAMETER_CODE);
		super.optionalCodes.add(RemoteOperations._PARAMETER_CODE);
		super.optionalCodes.add(ParameterCompatibilityInformation._PARAMETER_CODE);
		super.optionalCodes.add(GenericNotificationIndicator._PARAMETER_CODE);
		super.optionalCodes.add(ServiceActivation._PARAMETER_CODE);
		super.optionalCodes.add(GenericReference._PARAMETER_CODE);
		super.optionalCodes.add(MLPPPrecedence._PARAMETER_CODE);
		super.optionalCodes.add(TransimissionMediumRequierementPrime._PARAMETER_CODE);
		super.optionalCodes.add(LocationNumber._PARAMETER_CODE);
		super.optionalCodes.add(ForwardGVNS._PARAMETER_CODE);
		super.optionalCodes.add(CCSS._PARAMETER_CODE);
		super.optionalCodes.add(NetworkManagementControls._PARAMETER_CODE);

		super.optionalCodeToIndex.put(TransitNetworkSelection._PARAMETER_CODE, _INDEX_O_TransitNetworkSelection);
		super.optionalCodeToIndex.put(CallReference._PARAMETER_CODE, _INDEX_O_CallReference);
		super.optionalCodeToIndex.put(CallingPartyNumber._PARAMETER_CODE, _INDEX_O_CallingPartyNumber);
		super.optionalCodeToIndex.put(OptionalForwardCallIndicators._PARAMETER_CODE, _INDEX_O_OptionalForwardCallIndicators);
		super.optionalCodeToIndex.put(RedirectingNumber._PARAMETER_CODE, _INDEX_O_RedirectingNumber);
		super.optionalCodeToIndex.put(RedirectionInformation._PARAMETER_CODE, _INDEX_O_RedirectionInformation);
		super.optionalCodeToIndex.put(ClosedUserGroupInterlockCode._PARAMETER_CODE, _INDEX_O_ClosedUserGroupInterlockCode);
		super.optionalCodeToIndex.put(ConnectionRequest._PARAMETER_CODE, _INDEX_O_ConnectionRequest);
		super.optionalCodeToIndex.put(OriginalCalledNumberImpl._PARAMETER_CODE, _INDEX_O_OriginalCalledNumber);
		super.optionalCodeToIndex.put(UserToUserInformation._PARAMETER_CODE, _INDEX_O_UserToUserInformation);
		super.optionalCodeToIndex.put(AccessTransport._PARAMETER_CODE, _INDEX_O_AccessTransport);
		super.optionalCodeToIndex.put(UserServiceInformation._PARAMETER_CODE, _INDEX_O_UserServiceInformation);
		super.optionalCodeToIndex.put(UserToUserIndicators._PARAMETER_CODE, _INDEX_O_User2UIndicators);
		super.optionalCodeToIndex.put(GenericNumber._PARAMETER_CODE, _INDEX_O_GenericNumber);
		super.optionalCodeToIndex.put(PropagationDelayCounter._PARAMETER_CODE, _INDEX_O_PropagationDelayCounter);
		super.optionalCodeToIndex.put(UserServiceInformationPrime._PARAMETER_CODE, _INDEX_O_UserServiceInformationPrime);
		super.optionalCodeToIndex.put(NetworkSpecificFacility._PARAMETER_CODE, _INDEX_O_NetworkSPecificFacility);
		super.optionalCodeToIndex.put(GenericDigits._PARAMETER_CODE, _INDEX_O_GenericDigits);
		super.optionalCodeToIndex.put(OriginatingISCPointCode._PARAMETER_CODE, _INDEX_O_OriginatingISCPointCode);
		super.optionalCodeToIndex.put(UserTeleserviceInformation._PARAMETER_CODE, _INDEX_O_UserTeleserviceInformation);
		super.optionalCodeToIndex.put(RemoteOperations._PARAMETER_CODE, _INDEX_O_RemoteOperations);
		super.optionalCodeToIndex.put(ParameterCompatibilityInformation._PARAMETER_CODE, _INDEX_O_ParameterCompatibilityInformation);
		super.optionalCodeToIndex.put(GenericNotificationIndicator._PARAMETER_CODE, _INDEX_O_GenericNotificationIndicator);
		super.optionalCodeToIndex.put(ServiceActivation._PARAMETER_CODE, _INDEX_O_ServiceActivation);
		super.optionalCodeToIndex.put(GenericReference._PARAMETER_CODE, _INDEX_O_GenericReference);
		super.optionalCodeToIndex.put(MLPPPrecedence._PARAMETER_CODE, _INDEX_O_MLPPPrecedence);
		super.optionalCodeToIndex.put(TransimissionMediumRequierementPrime._PARAMETER_CODE, _INDEX_O_TransimissionMediumRequierementPrime);
		super.optionalCodeToIndex.put(LocationNumber._PARAMETER_CODE, _INDEX_O_LocationNumber);
		super.optionalCodeToIndex.put(ForwardGVNS._PARAMETER_CODE, _INDEX_O_ForwardGVNS);
		super.optionalCodeToIndex.put(CCSS._PARAMETER_CODE, _INDEX_O_CCSS);
		super.optionalCodeToIndex.put(NetworkManagementControls._PARAMETER_CODE, _INDEX_O_NetworkManagementControls);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.isup.messages.ISUPMessage#decodeMandatoryParameters(byte[],
	 * int)
	 */
	@Override
	protected int decodeMandatoryParameters(byte[] b, int index) throws ParameterRangeInvalidException {
		int localIndex = index;

		if (b.length - index > 1) {

			// Message Type
			if (b[index] != this._MESSAGE_CODE_IAM) {
				throw new ParameterRangeInvalidException("Message code is not: " + this._MESSAGE_CODE_IAM);
			}
			index++;

			try {
				byte[] natureOfConnectionIndicators = new byte[1];
				natureOfConnectionIndicators[0] = b[index++];

				NatureOfConnectionIndicatorsImpl _nai = new NatureOfConnectionIndicatorsImpl(natureOfConnectionIndicators);
				this.setNatureOfConnectionIndicators(_nai);
			} catch (Exception e) {
				// AIOOBE or IllegalArg
				throw new ParameterRangeInvalidException("Failed to parse NatureOfConnectionIndicators due to: ", e);
			}

			try {
				byte[] body = new byte[2];
				body[0] = b[index++];
				body[1] = b[index++];

				ForwardCallIndicatorsImpl v = new ForwardCallIndicatorsImpl(body);
				this.setForwardCallIndicators(v);
			} catch (Exception e) {
				// AIOOBE or IllegalArg
				throw new ParameterRangeInvalidException("Failed to parse ForwardCallIndicators due to: ", e);
			}

			try {
				byte[] body = new byte[1];
				body[0] = b[index++];

				CallingPartyCategoryImpl v = new CallingPartyCategoryImpl(body);
				this.setCallingPartCategory(v);
			} catch (Exception e) {
				// AIOOBE or IllegalArg
				throw new ParameterRangeInvalidException("Failed to parse CallingPartyCategory due to: ", e);
			}
			try {
				byte[] body = new byte[1];
				body[0] = b[index++];

				TransmissionMediumRequirementImpl v = new TransmissionMediumRequirementImpl(body);
				this.setTransmissionMediumRequirement(v);
			} catch (Exception e) {
				// AIOOBE or IllegalArg
				throw new ParameterRangeInvalidException("Failed to parse TransmissionMediumRequirement due to: ", e);
			}

			return index - localIndex;
		} else {
			throw new ParameterRangeInvalidException("byte[] must have atleast two octets");
		}
	}

	/**
	 * @param parameterBody
	 * @param parameterCode
	 * @throws ParameterRangeInvalidException
	 */
	protected void decodeMandatoryVariableBody(byte[] parameterBody, int parameterIndex) throws ParameterRangeInvalidException {
		switch (parameterIndex) {
		case _INDEX_V_CalledPartyNumber:
			CalledPartyNumberImpl cpn = new CalledPartyNumberImpl(parameterBody);
			this.setCalledPartyNumber(cpn);
			break;
		default:
			throw new ParameterRangeInvalidException("Unrecognized parameter index for mandatory variable part: " + parameterIndex);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.messages.ISUPMessage#decodeOptionalBody(byte[],
	 * byte)
	 */
	@Override
	protected void decodeOptionalBody(byte[] parameterBody, byte parameterCode) throws ParameterRangeInvalidException {

		// TODO Auto-generated method stub
		switch ((int) parameterCode) {
		case TransitNetworkSelectionImpl._PARAMETER_CODE:
			TransitNetworkSelectionImpl v = new TransitNetworkSelectionImpl(parameterBody);
			setTransitNetworkSelection(v);
			break;
		case CallReferenceImpl._PARAMETER_CODE:
			CallReferenceImpl cr = new CallReferenceImpl(parameterBody);
			this.setCallReference(cr);
			break;
		case CallingPartyNumberImpl._PARAMETER_CODE:
			CallingPartyNumberImpl cpn = new CallingPartyNumberImpl(parameterBody);
			this.setCallingPartyNumber(cpn);
			break;
		case OptionalForwardCallIndicatorsImpl._PARAMETER_CODE:
			OptionalForwardCallIndicatorsImpl ofci = new OptionalForwardCallIndicatorsImpl(parameterBody);
			this.setOptForwardCallIndicators(ofci);
			break;
		case RedirectingNumberImpl._PARAMETER_CODE:
			RedirectingNumberImpl rn = new RedirectingNumberImpl(parameterBody);
			this.setRedirectingNumber(rn);
			break;
		case RedirectionInformationImpl._PARAMETER_CODE:
			RedirectionInformationImpl ri = new RedirectionInformationImpl(parameterBody);
			this.setRedirectionInformation(ri);
			break;
		case ClosedUserGroupInterlockCodeImpl._PARAMETER_CODE:
			ClosedUserGroupInterlockCodeImpl cugic = new ClosedUserGroupInterlockCodeImpl(parameterBody);
			this.setCUserGroupInterlockCode(cugic);
			break;
		case ConnectionRequestImpl._PARAMETER_CODE:
			ConnectionRequestImpl cr2 = new ConnectionRequestImpl(parameterBody);
			this.setConnectionRequest(cr2);
			break;
		case OriginalCalledNumberImpl._PARAMETER_CODE:
			OriginalCalledNumberImpl orn = new OriginalCalledNumberImpl(parameterBody);
			this.setOriginalCalledNumber(orn);
			break;
		case UserToUserInformationImpl._PARAMETER_CODE:
			UserToUserInformationImpl u2ui = new UserToUserInformationImpl(parameterBody);
			this.setU2UInformation(u2ui);
			break;
		case AccessTransportImpl._PARAMETER_CODE:
			AccessTransportImpl at = new AccessTransportImpl(parameterBody);
			this.setAccessTransport(at);
			break;
		case UserServiceInformationImpl._PARAMETER_CODE:
			UserServiceInformationImpl usi = new UserServiceInformationImpl(parameterBody);
			this.setUserServiceInformation(usi);
			break;
		case UserToUserIndicatorsImpl._PARAMETER_CODE:
			UserToUserIndicatorsImpl utui = new UserToUserIndicatorsImpl(parameterBody);
			this.setU2UIndicators(utui);
			break;
		case GenericNumberImpl._PARAMETER_CODE:
			GenericNumberImpl gn = new GenericNumberImpl(parameterBody);
			this.setGenericNumber(gn);
			break;
		case PropagationDelayCounterImpl._PARAMETER_CODE:
			PropagationDelayCounterImpl pdc = new PropagationDelayCounterImpl(parameterBody);
			this.setPropagationDelayCounter(pdc);
			break;
		case UserServiceInformationPrimeImpl._PARAMETER_CODE:
			UserServiceInformationPrimeImpl usip = new UserServiceInformationPrimeImpl(parameterBody);
			this.setUserServiceInformationPrime(usip);
			break;
		case NetworkSpecificFacilityImpl._PARAMETER_CODE:
			NetworkSpecificFacilityImpl nsf = new NetworkSpecificFacilityImpl(parameterBody);
			this.setNetworkSpecificFacility(nsf);
			break;
		case GenericDigitsImpl._PARAMETER_CODE:
			GenericDigitsImpl gd = new GenericDigitsImpl(parameterBody);
			this.setGenericDigits(gd);
			break;
		case OriginatingISCPointCodeImpl._PARAMETER_CODE:
			OriginatingISCPointCodeImpl vv = new OriginatingISCPointCodeImpl(parameterBody);
			this.setOriginatingISCPointCode(vv);
			break;
		case UserTeleserviceInformationImpl._PARAMETER_CODE:
			UserTeleserviceInformationImpl uti = new UserTeleserviceInformationImpl(parameterBody);
			this.setUserTeleserviceInformation(uti);
			break;
		case RemoteOperationsImpl._PARAMETER_CODE:
			RemoteOperationsImpl ro = new RemoteOperationsImpl(parameterBody);
			this.setRemoteOperations(ro);
			break;
		case ParameterCompatibilityInformationImpl._PARAMETER_CODE:
			ParameterCompatibilityInformationImpl pci = new ParameterCompatibilityInformationImpl(parameterBody);
			this.setParameterCompatibilityInformation(pci);
			break;
		case GenericNotificationIndicatorImpl._PARAMETER_CODE:
			GenericNotificationIndicatorImpl gni = new GenericNotificationIndicatorImpl(parameterBody);
			this.setGenericNotificationIndicator(gni);
			break;
		case ServiceActivationImpl._PARAMETER_CODE:
			ServiceActivationImpl sa = new ServiceActivationImpl(parameterBody);
			this.setServiceActivation(sa);
			break;
		case GenericReferenceImpl._PARAMETER_CODE:
			GenericReferenceImpl gr = new GenericReferenceImpl(parameterBody);
			this.setGenericReference(gr);
			break;
		case MLPPPrecedenceImpl._PARAMETER_CODE:
			MLPPPrecedenceImpl mlpp = new MLPPPrecedenceImpl(parameterBody);
			this.setMLPPPrecedence(mlpp);

		case TransmissionMediumRequirementImpl._PARAMETER_CODE:
			TransmissionMediumRequirementImpl tmr = new TransmissionMediumRequirementImpl(parameterBody);
			this.setTransmissionMediumRequirement(tmr);
		case LocationNumberImpl._PARAMETER_CODE:
			LocationNumberImpl ln = new LocationNumberImpl(parameterBody);
			this.setLocationNumber(ln);
		case ForwardGVNSImpl._PARAMETER_CODE:
			ForwardGVNSImpl fgvns = new ForwardGVNSImpl(parameterBody);
			this.setForwardGVNS(fgvns);
		case CCSSImpl._PARAMETER_CODE:
			CCSSImpl ccss = new CCSSImpl(parameterBody);
			this.setCCSS(ccss);
		case NetworkManagementControlsImpl._PARAMETER_CODE:
			NetworkManagementControlsImpl nmc = new NetworkManagementControlsImpl(parameterBody);
			this.setNetworkManagementControls(nmc);
			break;
		default:
			throw new IllegalArgumentException("Unrecognized parameter code for optional part: " + parameterCode);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.mobicents.isup.messages.ISUPMessage#
	 * getNumberOfMandatoryVariableLengthParameters()
	 */
	@Override
	protected int getNumberOfMandatoryVariableLengthParameters() {

		return _MANDATORY_VAR_COUNT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.messages.ISUPMessage#getMessageType()
	 */
	@Override
	public MessageType getMessageType() {
		return this._MESSAGE_TYPE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.messages.ISUPMessage#hasAllMandatoryParameters()
	 */
	@Override
	public boolean hasAllMandatoryParameters() {
		if (!super.f_Parameters.keySet().containsAll(mandatoryParam) || super.f_Parameters.values().contains(null)) {
			return false;
		}
		if (!super.v_Parameters.containsKey(_INDEX_V_CalledPartyNumber) || super.v_Parameters.get(_INDEX_V_CalledPartyNumber) == null) {
			return false;
		}
		return true;
	}

	public NatureOfConnectionIndicators getNatureOfConnectionIndicators() {
		return (NatureOfConnectionIndicators) super.f_Parameters.get(_INDEX_F_NatureOfConnectionIndicators);
	}

	public void setNatureOfConnectionIndicators(NatureOfConnectionIndicators v) {
		super.f_Parameters.put(_INDEX_F_NatureOfConnectionIndicators, v);
	}

	public ForwardCallIndicators getForwardCallIndicators() {
		return (ForwardCallIndicators) super.f_Parameters.get(_INDEX_F_ForwardCallIndicators);
	}

	public void setForwardCallIndicators(ForwardCallIndicators v) {
		super.f_Parameters.put(_INDEX_F_ForwardCallIndicators, v);
	}

	public CallingPartyCategory getCallingPartCategory() {
		return (CallingPartyCategory) super.f_Parameters.get(_INDEX_F_CallingPartyCategory);
	}

	public void setCallingPartCategory(CallingPartyCategory v) {
		super.f_Parameters.put(_INDEX_F_CallingPartyCategory, v);
	}

	public TransmissionMediumRequirement getTransmissionMediumRequirement() {
		return (TransmissionMediumRequirement) super.f_Parameters.get(_INDEX_F_TransmissionMediumRequirement);
	}

	public void setTransmissionMediumRequirement(TransmissionMediumRequirement v) {
		super.f_Parameters.put(_INDEX_F_TransmissionMediumRequirement, v);
	}

	public CalledPartyNumber getCalledPartyNumber() {
		return (CalledPartyNumber) super.v_Parameters.get(_INDEX_V_CalledPartyNumber);
	}

	public void setCalledPartyNumber(CalledPartyNumber v) {
		super.v_Parameters.put(_INDEX_V_CalledPartyNumber, v);
	}

	public TransitNetworkSelection getTransitNetworkSelection() {
		return (TransitNetworkSelection) super.o_Parameters.get(_INDEX_O_TransitNetworkSelection);
	}

	public void setTransitNetworkSelection(TransitNetworkSelection v) {
		super.o_Parameters.put(_INDEX_O_TransitNetworkSelection, v);
	}

	public CallReference getCallReference() {
		return (CallReference) super.o_Parameters.get(_INDEX_O_CallReference);
	}

	public void setCallReference(CallReference v) {
		super.o_Parameters.put(_INDEX_O_CallReference, v);
	}

	public CallingPartyNumber getCallingPartyNumber() {
		return (CallingPartyNumber) super.o_Parameters.get(_INDEX_O_CallingPartyNumber);
	}

	public void setCallingPartyNumber(CallingPartyNumber v) {
		super.o_Parameters.put(_INDEX_O_CallingPartyNumber, v);
	}

	public OptionalForwardCallIndicators getOptForwardCallIndicators() {
		return (OptionalForwardCallIndicators) super.o_Parameters.get(_INDEX_O_OptionalForwardCallIndicators);
	}

	public void setOptForwardCallIndicators(OptionalForwardCallIndicators v) {
		super.o_Parameters.put(_INDEX_O_OptionalForwardCallIndicators, v);
	}

	public RedirectingNumber getRedirectingNumber() {
		return (RedirectingNumber) super.o_Parameters.get(_INDEX_O_RedirectingNumber);
	}

	public void setRedirectingNumber(RedirectingNumber v) {
		super.o_Parameters.put(_INDEX_O_RedirectingNumber, v);
	}

	public RedirectionInformation getRedirectionInformation() {
		return (RedirectionInformation) super.o_Parameters.get(_INDEX_O_RedirectionInformation);
	}

	public void setRedirectionInformation(RedirectionInformation v) {
		super.o_Parameters.put(_INDEX_O_RedirectionInformation, v);
	}

	public ClosedUserGroupInterlockCode getCUserGroupInterlockCode() {
		return (ClosedUserGroupInterlockCode) super.o_Parameters.get(_INDEX_O_ClosedUserGroupInterlockCode);
	}

	public void setCUserGroupInterlockCode(ClosedUserGroupInterlockCode v) {
		super.o_Parameters.put(_INDEX_O_ClosedUserGroupInterlockCode, v);
	}

	public ConnectionRequest getConnectionRequest() {
		return (ConnectionRequest) super.o_Parameters.get(_INDEX_O_ConnectionRequest);
	}

	public void setConnectionRequest(ConnectionRequest v) {
		super.o_Parameters.put(_INDEX_O_ConnectionRequest, v);
	}

	public OriginalCalledNumber getOriginalCalledNumber() {
		return (OriginalCalledNumberImpl) super.o_Parameters.get(_INDEX_O_OriginalCalledNumber);
	}

	public void setOriginalCalledNumber(OriginalCalledNumber v) {
		super.o_Parameters.put(_INDEX_O_OriginalCalledNumber, v);
	}

	public UserToUserInformation getU2UInformation() {
		return (UserToUserInformation) super.o_Parameters.get(_INDEX_O_UserToUserInformation);
	}

	public void setU2UInformation(UserToUserInformation v) {
		super.o_Parameters.put(_INDEX_O_UserToUserInformation, v);
	}

	public UserServiceInformation getUserServiceInformation() {
		return (UserServiceInformation) super.o_Parameters.get(_INDEX_O_UserServiceInformation);
	}

	public void setUserServiceInformation(UserServiceInformation v) {
		super.o_Parameters.put(_INDEX_O_UserServiceInformation, v);
	}

	public NetworkSpecificFacility getNetworkSpecificFacility() {
		return (NetworkSpecificFacility) super.o_Parameters.get(_INDEX_O_NetworkSPecificFacility);
	}

	public void setNetworkSpecificFacility(NetworkSpecificFacility v) {
		super.o_Parameters.put(_INDEX_O_NetworkSPecificFacility, v);
	}

	public GenericDigits getGenericDigits() {
		return (GenericDigits) super.o_Parameters.get(_INDEX_O_GenericDigits);
	}

	public void setGenericDigits(GenericDigits v) {
		super.o_Parameters.put(_INDEX_O_GenericDigits, v);
	}

	public OriginatingISCPointCode getOriginatingISCPointCode() {
		return (OriginatingISCPointCode) super.o_Parameters.get(_INDEX_O_OriginatingISCPointCode);
	}

	public void setOriginatingISCPointCode(OriginatingISCPointCode v) {
		super.o_Parameters.put(_INDEX_O_OriginatingISCPointCode, v);
	}

	public UserTeleserviceInformation getUserTeleserviceInformation() {
		return (UserTeleserviceInformation) super.o_Parameters.get(_INDEX_O_UserTeleserviceInformation);
	}

	public void setUserTeleserviceInformation(UserTeleserviceInformation v) {
		super.o_Parameters.put(_INDEX_O_UserTeleserviceInformation, v);
	}

	public RemoteOperations getRemoteOperations() {
		return (RemoteOperations) super.o_Parameters.get(_INDEX_O_RemoteOperations);
	}

	public void setRemoteOperations(RemoteOperations v) {
		super.o_Parameters.put(_INDEX_O_RemoteOperations, v);
	}

	public ParameterCompatibilityInformation getParameterCompatibilityInformation() {
		return (ParameterCompatibilityInformation) super.o_Parameters.get(_INDEX_O_ParameterCompatibilityInformation);
	}

	public void setParameterCompatibilityInformation(ParameterCompatibilityInformation v) {
		super.o_Parameters.put(_INDEX_O_ParameterCompatibilityInformation, v);
	}

	public GenericNotificationIndicator getGenericNotificationIndicator() {
		return (GenericNotificationIndicator) super.o_Parameters.get(_INDEX_O_GenericNotificationIndicator);
	}

	public void setGenericNotificationIndicator(GenericNotificationIndicator v) {
		super.o_Parameters.put(_INDEX_O_GenericNotificationIndicator, v);
	}

	public ServiceActivation getServiceActivation() {
		return (ServiceActivation) super.o_Parameters.get(_INDEX_O_ServiceActivation);
	}

	public void setServiceActivation(ServiceActivation v) {
		super.o_Parameters.put(_INDEX_O_ServiceActivation, v);
	}

	public GenericReference getGenericReference() {
		return (GenericReference) super.o_Parameters.get(_INDEX_O_GenericReference);
	}

	public void setGenericReference(GenericReference v) {
		super.o_Parameters.put(_INDEX_O_GenericReference, v);
	}

	public MLPPPrecedence getMLPPPrecedence() {
		return (MLPPPrecedence) super.o_Parameters.get(_INDEX_O_MLPPPrecedence);
	}

	public void setMLPPPrecedence(MLPPPrecedence v) {
		super.o_Parameters.put(_INDEX_O_MLPPPrecedence, v);
	}

	public TransimissionMediumRequierementPrime getTransimissionMediumReqPrime() {
		return (TransimissionMediumRequierementPrime) super.o_Parameters.get(_INDEX_O_TransimissionMediumRequierementPrime);
	}

	public void setTransimissionMediumReqPrime(TransimissionMediumRequierementPrime v) {
		super.o_Parameters.put(_INDEX_O_TransimissionMediumRequierementPrime, v);
	}

	public LocationNumber getLocationNumber() {
		return (LocationNumber) super.o_Parameters.get(_INDEX_O_LocationNumber);
	}

	public void setLocationNumber(LocationNumber v) {
		super.o_Parameters.put(_INDEX_O_LocationNumber, v);
	}

	public ForwardGVNS getForwardGVNS() {
		return (ForwardGVNS) super.o_Parameters.get(_INDEX_O_ForwardGVNS);
	}

	public void setForwardGVNS(ForwardGVNS v) {
		super.o_Parameters.put(_INDEX_O_ForwardGVNS, v);
	}

	public CCSS getCCSS() {
		return (CCSS) super.o_Parameters.get(_INDEX_O_CCSS);
	}

	public void setCCSS(CCSS v) {
		super.o_Parameters.put(_INDEX_O_CCSS, v);
	}

	public NetworkManagementControls getNetworkManagementControls() {
		return (NetworkManagementControls) super.o_Parameters.get(_INDEX_O_NetworkManagementControls);
	}

	public void setNetworkManagementControls(NetworkManagementControls v) {
		super.o_Parameters.put(_INDEX_O_NetworkManagementControls, v);
	}

	/**
	 * @param usip
	 */
	public void setUserServiceInformationPrime(UserServiceInformationPrime v) {
		super.o_Parameters.put(_INDEX_O_UserServiceInformationPrime, v);
	}

	public UserServiceInformationPrime getUserServiceInformationPrime() {
		return (UserServiceInformationPrime) super.o_Parameters.get(_INDEX_O_UserServiceInformationPrime);
	}

	/**
	 * @param pdc
	 */
	public void setPropagationDelayCounter(PropagationDelayCounter v) {
		super.o_Parameters.put(_INDEX_O_PropagationDelayCounter, v);

	}

	public PropagationDelayCounter getPropagationDelayCounter() {
		return (PropagationDelayCounter) super.o_Parameters.get(_INDEX_O_PropagationDelayCounter);
	}

	/**
	 * @param gn
	 */
	public void setGenericNumber(GenericNumber v) {
		super.o_Parameters.put(_INDEX_O_GenericNumber, v);

	}

	public GenericNumber getGenericNumber() {
		return (GenericNumber) super.o_Parameters.get(_INDEX_O_GenericNumber);
	}

	/**
	 * @param utui
	 */
	public void setU2UIndicators(UserToUserIndicators v) {
		super.o_Parameters.put(_INDEX_O_User2UIndicators, v);

	}

	public UserToUserIndicators getU2UIndicators() {
		return (UserToUserIndicators) super.o_Parameters.get(_INDEX_O_User2UIndicators);
	}

	/**
	 * @param at
	 */
	public void setAccessTransport(AccessTransport v) {
		super.o_Parameters.put(_INDEX_O_AccessTransport, v);

	}

	public AccessTransport getAccessTransport() {
		return (AccessTransport) super.o_Parameters.get(_INDEX_O_AccessTransport);
	}

}
