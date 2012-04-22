package com.agorikov.rsdnhome.webclient.model;

import java.io.IOException;
import java.util.Map;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.Transport;
import org.xmlpull.v1.XmlPullParserException;

import com.agorikov.rsdnhome.common.util.Log;
import com.agorikov.rsdnhome.model.RowVersionProvider;

public abstract class WebMethod {
	static final String TAG = "WebMethod";
	
	protected final String SOAP_ACTION_NS;
	protected final String methodName;
	private boolean notContinueFlag;
	protected WebMethod(final String SOAP_ACTION_NS, final String methodName) {
		this.SOAP_ACTION_NS = SOAP_ACTION_NS;
		this.methodName = methodName;
	}
	
	
	/**
	 * Returns true if has more data
	 * @param arg
	 * @param recv
	 * @return
	 */
	protected final boolean call(final SoapObject arg, final RowVersionProvider ...rowversion) {
		boolean finished = true;
		
		final SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
		envelope.dotNet = true;
		envelope.implicitTypes = true;
		if (arg != null) {
			for (final RowVersionProvider ver : rowversion) {
				final String lastRowVersionKey = ver.getRequestName();
				final String lastRowVersionValue = ver.get();
				final SoapPrimitive primitive = new SoapPrimitive(envelope.enc, "base64", lastRowVersionValue);
				Log.d(TAG, String.format("Sending %s : %s", lastRowVersionKey, lastRowVersionValue));
				arg.addProperty(lastRowVersionKey, primitive);
			}
		}
		final SoapObject request = new SoapObject(SOAP_ACTION_NS, methodName);
		if (arg != null)
			request.addSoapObject(arg);
		envelope.setOutputSoapObject(request);

		final Transport transport = getTransport();

		try {
			WebMethod.this.notContinueFlag = false;
			transport.call(SOAP_ACTION_NS + methodName, envelope);
			boolean doNotContinue = this.notContinueFlag;
			final Map<RowVersionProvider, String> newRowVersion = getReceivedRowVersion();
			
			for (final RowVersionProvider ver : rowversion) {
				final String lastRowVersionValue = newRowVersion.get(ver);
				if (lastRowVersionValue != null && lastRowVersionValue.length() != 0) {
					final String lastRowVersionKey = ver.getResponseName();
					Log.d(TAG, String.format("Received %s : %s", lastRowVersionKey, lastRowVersionValue));
					if (!lastRowVersionValue.equals(ver.get())) {
						finished = false;
						ver.put(lastRowVersionValue);
					}
				}
			}
			finished |= doNotContinue;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		} catch (final XmlPullParserException e) {
			throw new RuntimeException(e);
		}
		return !finished;
	}

	protected abstract Map<RowVersionProvider, String> getReceivedRowVersion();


	protected final void setNotContinueFlag(final boolean notContinue) {
		this.notContinueFlag = notContinue;
	}


	protected abstract Transport getTransport();
}
