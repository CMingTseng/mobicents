/**
 * Start time:12:39:34 2009-04-02<br>
 * Project: mobicents-isup-stack<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.isup.parameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Start time:12:39:34 2009-04-02<br>
 * Project: mobicents-isup-stack<br>
 * This is composed param ?
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 */
public class ParameterCompatibilityInformation extends AbstractParameter {

	public static final int _PARAMETER_CODE = 0x39;
	private List<Byte> parameterCodes = new ArrayList<Byte>();
	private List<InstructionIndicators> instructionIndicators = new ArrayList<InstructionIndicators>();

	public ParameterCompatibilityInformation(byte[] b) {
		super();
		decodeElement(b);
	}

	public ParameterCompatibilityInformation() {
		super();
		// TODO Auto-generated constructor stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#decodeElement(byte[])
	 */
	public int decodeElement(byte[] b) throws IllegalArgumentException {

		if (b == null || b.length < 2) {
			throw new IllegalArgumentException("byte[] must  not be null and length must  greater than 1");
		}

		
		
		ByteArrayOutputStream bos = null;
		boolean newParameter = true;
		byte parameterCode = 0;

		for (int index = 0; index < b.length; index++) {
			if (newParameter) {
				parameterCode = b[index];
				bos = new ByteArrayOutputStream();
				newParameter = false;
				continue;
			} else {
				bos.write(b[index]);

				if (((b[index] >> 7) & 0x01) == 0) {
					// ext bit is zero, this is last octet
					
					if (bos.size() < 3) {
						this.addInstructions(parameterCode, new InstructionIndicators(bos.toByteArray()));
					} else {
						this.addInstructions(parameterCode, new InstructionIndicators(bos.toByteArray(), true));
					}
					newParameter = true;
				} else {
				
					continue;
				}
			}

		}
		
	
		return b.length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.isup.ISUPComponent#encodeElement()
	 */
	public byte[] encodeElement() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		for (int index = 0; index < this.parameterCodes.size(); index++) {
			bos.write(this.parameterCodes.get(index).byteValue());
			bos.write(this.instructionIndicators.get(index).encodeElement());
		}
		return bos.toByteArray();
	}

	public void addInstructions(Byte parameterCode, InstructionIndicators instructionIndicators) {
		// FIXME: do we need to check for duplicate?
		this.parameterCodes.add(parameterCode);
		this.instructionIndicators.add(instructionIndicators);
	}

	// FIXME: Crude API
	public InstructionIndicators getInstructionIndicators(int index) {
		return this.instructionIndicators.get(index);
	}

	public Byte getParameterCode(int index) {
		return this.parameterCodes.get(index);
	}

	public int size() {
		return this.instructionIndicators.size();
	}

	public void remove(int index) {
		this.instructionIndicators.remove(index);
		this.parameterCodes.remove(index);
	}

	public int getCode() {

		return _PARAMETER_CODE;
	}
}
