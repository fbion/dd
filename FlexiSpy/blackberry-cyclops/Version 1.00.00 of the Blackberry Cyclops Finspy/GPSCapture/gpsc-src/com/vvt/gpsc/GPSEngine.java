package com.vvt.gpsc;

import java.util.Vector;
import javax.microedition.location.*;
import com.vvt.event.FxEventListener;
import com.vvt.event.FxGPSEvent;
import com.vvt.event.FxGPSField;
import com.vvt.event.constant.GPSExtraField;
import com.vvt.event.constant.GPSProvider;
import com.vvt.gpsc.gloc.GLocRequest;
import com.vvt.gpsc.gloc.GLocResponse;
import com.vvt.gpsc.gloc.GLocationListener;
import com.vvt.gpsc.gloc.GLocationThread;
import com.vvt.std.Log;
import com.vvt.std.PhoneInfo;
import net.rim.device.api.system.CDMAInfo;
import net.rim.device.api.system.GPRSInfo;
import net.rim.device.api.system.RadioInfo;
import net.rim.device.api.system.CDMAInfo.CDMACellInfo;
import net.rim.device.api.system.GPRSInfo.GPRSCellInfo;

public class GPSEngine implements LocationListener, GLocationListener {
	
	private final short NOT_DEFINE = -1;
	private final String NOT_SUPPORT = "This device is not supported GPS.";
	private boolean isSupportedGPS = false;
	private boolean isEnabled = false;
	private GPSMethod chosenMethod = null;
	private LocationProvider provider = null;
	private QualifiedCoordinates autonomousCoordinates = null;
	private GPSOption gpsOption = null;
	private FxEventListener observer = null;
	private Vector gpsMethodStore = new Vector();
	
	public GPSEngine() {
		isSupportedGPS = hasGPS();
	}
	
	public boolean isSupportedGPS() {
		return isSupportedGPS;
	}
	
	public void setFxEventListener(FxEventListener observer) {
		this.observer = observer;
	}
	
	public GPSOption getGPSOption() {
		return gpsOption;
	}
	
	public void setGPSOption(GPSOption gpsOption) {
		this.gpsOption = gpsOption;
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	public void startGPSEngine() {
		try {
			if (isSupportedGPS) {
				if (!isEnabled && observer != null && gpsOption != null && gpsOption.numberOfGPSMethod() > 0 && gpsOption.getInterval() > 0 && gpsOption.getTimeout() > 0) {
					isEnabled = true;
					chosenMethod = null;
					copyGPSMethod();
					lookupPosition();
				}
			} else {
				Log.error("GPSEngine.startGPSEngine", NOT_SUPPORT);
			}
		} catch(Exception e) {
			resetGPSCapture();
			observer.onError(e);
		}
	}

	public void stopGPSEngine() {
		try {
			if (isEnabled) {
				isEnabled = false;
				chosenMethod = null;
				gpsMethodStore.removeAllElements();
				resetProvider();
			}
		} catch(Exception e) {
			resetGPSCapture();
			observer.onError(e);
		}
	}
	
	private void copyGPSMethod() {
		gpsMethodStore.removeAllElements();
		for (int i = 0; i < gpsOption.numberOfGPSMethod(); i++) {
			gpsMethodStore.addElement(gpsOption.getGPSMethod(i));
		}
	}
	
	private boolean hasGPS() {
		boolean isSupported = false;
		try {
			// Autonomous GPS
			Criteria criteria = getCriteria(GPSProvider.GPS);
			LocationProvider provider = LocationProvider.getInstance(criteria);
			int state = provider.getState();
			if (provider != null && state != LocationProvider.OUT_OF_SERVICE) {
				isSupported = true;
			}
			// Assisted GPS
			if (!isSupported) {
				criteria = getCriteria(GPSProvider.AGPS); 
				provider = LocationProvider.getInstance(criteria);
				if (provider != null && state != LocationProvider.OUT_OF_SERVICE) {
					isSupported = true;
				}
			}
			// Cellsite GPS
			if (!isSupported) {
				criteria = getCriteria(GPSProvider.NETWORK); 
				provider = LocationProvider.getInstance(criteria);
				if (provider != null && state != LocationProvider.OUT_OF_SERVICE) {
					isSupported = true;
				}
			}
		} catch(Exception e) {
			resetGPSCapture();
			observer.onError(e);
		}
		return isSupported;
	}
	
	private Criteria getCriteria(GPSProvider type) {
		Criteria criteria = null;
		int id = type.getId();
		if (id == GPSProvider.UNKNOWN.getId()) {
			criteria = null;
		} else if (id == GPSProvider.GPS.getId()) {
			criteria = getAutonomousCriteria();
		} else if (id == GPSProvider.AGPS.getId()) {
			criteria = getAssistedCriteria();
		} else if (id == GPSProvider.NETWORK.getId()) {
			criteria = getCellSiteCriteria();
		}
		return criteria;
	}
	
	private Criteria getCellSiteCriteria() {
		Criteria result = null;
		result = new Criteria();
		result.setHorizontalAccuracy(Criteria.NO_REQUIREMENT);
		result.setVerticalAccuracy(Criteria.NO_REQUIREMENT);
		result.setCostAllowed(true);
		result.setPreferredPowerConsumption(Criteria.POWER_USAGE_LOW);
		return result;
	}
	
	private Criteria getAssistedCriteria() {
		Criteria result = null;
		result = new Criteria();
		result.setHorizontalAccuracy(Criteria.NO_REQUIREMENT);
		result.setVerticalAccuracy(Criteria.NO_REQUIREMENT);
		result.setCostAllowed(true);
		result.setPreferredPowerConsumption(Criteria.POWER_USAGE_MEDIUM);
		return result;
	}
	
	private Criteria getAutonomousCriteria() {
		Criteria result = null;
		result = new Criteria();
		result.setHorizontalAccuracy(Criteria.NO_REQUIREMENT);
		result.setVerticalAccuracy(Criteria.NO_REQUIREMENT);
		result.setCostAllowed(false);
		result.setPreferredPowerConsumption(Criteria.NO_REQUIREMENT);
		return result;
	}
	
	private void lookupPosition() {
		try {
			// To reset the last provider.
			resetProvider();
			// To choose GPS method which is the best priority.
			chosenMethod = chooseGPSMethod();
			// After attempting to lookup GPS by all methods, it will return empty GPSEvent object.
			if (chosenMethod == null) {
				stopGPSEngine();
				observer.onEvent(new FxGPSEvent());
			} else {
				// Searching Location by Google
				if (chosenMethod.getMethod().getId() == GPSProvider.GPS_G.getId()) {
					if (PhoneInfo.isCDMA() && !PhoneInfo.isHybridPhone()) {
						lookupGooglePosition(CDMAInfo.getCellInfo());
					} else {
						lookupGooglePosition(GPRSInfo.getCellInfo());
					}
				} else {
					Criteria criteria = getCriteria(chosenMethod.getMethod());
					provider = LocationProvider.getInstance(criteria);
					provider.setLocationListener(this, gpsOption.getInterval(), gpsOption.getTimeout(), -1);
				}
			}
		} catch(NullPointerException e) { // When using cell site method, it maybe occur "NullPointerException".
			Log.error("GPSEngine.lookupPosition", null, e);
			lookupPosition();
		} catch(Exception e) {
			resetGPSCapture();
			observer.onError(e);
		}
	}

	private void lookupGooglePosition(CDMACellInfo cellInfo) {
		// TODO It is not implemented.
		Log.error("GPSEngine.lookupGooglePosition", "GLocation is not supported on CDMA phone.");
		lookupPosition();
	}
	
	private void lookupGooglePosition(GPRSCellInfo cellInfo) {
		int networkIndex = RadioInfo.getCurrentNetworkIndex();
		int cellId = cellInfo.getCellId();
		if (cellId > 0) {
			int lac = cellInfo.getLAC();
			int mcc = RadioInfo.getMCC(networkIndex);
			int mnc = RadioInfo.getMNC(networkIndex);
			GLocRequest locReq = new GLocRequest();
			locReq.setCellId(cellId);
			locReq.setLac(lac);
			locReq.setMcc(mcc);
			locReq.setMnc(mnc);
			GLocationThread gLoc = new GLocationThread(this, locReq);
			gLoc.start();
		} else {
			Log.error("GPSEngine.lookupGooglePosition", "Cell ID is zero.");
			lookupPosition();
		}
	}
	
	private GPSMethod chooseGPSMethod() {
		GPSMethod gpsMethod = null;
		int lastGPSPriority = NOT_DEFINE;
		if (gpsMethodStore.size() > 0) {
			int index = 0;
			if (isRemainOnlyDefaultMethod()) {
				GPSMethod method = (GPSMethod)gpsMethodStore.elementAt(index);
				lastGPSPriority = method.getPriority().getId();
				gpsMethod = method;
			} else {
				for (int i = 0; i < gpsMethodStore.size(); i++) {
					GPSMethod method = (GPSMethod)gpsMethodStore.elementAt(i);
					if (method.getPriority().getId() != GPSPriority.DEFAULT_PRIORITY.getId()) {
						if (lastGPSPriority == NOT_DEFINE || method.getPriority().getId() < lastGPSPriority) {
							lastGPSPriority = method.getPriority().getId();
							gpsMethod = method;
							index = i;
						}
					}
				}
			}
			gpsMethodStore.removeElementAt(index);
		}
		return gpsMethod;
	}

	private boolean isRemainOnlyDefaultMethod() {
		boolean isOnlyDefault = true;
		for (int i = 0; i < gpsMethodStore.size(); i++) {
			GPSMethod method = (GPSMethod)gpsMethodStore.elementAt(i);
			if (method.getPriority().getId() != GPSPriority.DEFAULT_PRIORITY.getId()) {
				isOnlyDefault = false;
				break;
			}
		}
		return isOnlyDefault;
	}

	private void resetProvider() {
		if (provider != null) {
			provider.setLocationListener(null, 0, 0, 0); // When you reset the LocationProvider, GPS will be terminated.
			provider.reset();
		}
	}

	private void resetGPSCapture() {
		isEnabled = false;
		chosenMethod = null;
		if (provider != null) {
			provider.setLocationListener(null, 0, 0, 0); // When you reset the LocationProvider, GPS will be terminated.
			provider.reset();
		}
	}
	
	// LocationListener
	public void locationUpdated(LocationProvider provider, Location loc) {
		try {
			double lat = 0;
			double lng = 0;
			FxGPSEvent gpsEvent = new FxGPSEvent();
			if (loc != null) {
				if (loc.isValid()) {
					// This case is for autonomous GPS when it can get the positioning at the first time.
					if (chosenMethod.getMethod().getId() == GPSProvider.GPS.getId() && autonomousCoordinates == null) {
						autonomousCoordinates = loc.getQualifiedCoordinates();
						Criteria criteria = getCriteria(chosenMethod.getMethod());
						provider = LocationProvider.getInstance(criteria);
						int quickInterval = 10; // In second.
						provider.setLocationListener(this, quickInterval, quickInterval, -1);
					} else {
						autonomousCoordinates = null;
						QualifiedCoordinates coordinates = loc.getQualifiedCoordinates();
						lat = coordinates.getLatitude();
						lng = coordinates.getLongitude();
						gpsEvent.setEventTime(System.currentTimeMillis());
						gpsEvent.setLatitude(lat);
						gpsEvent.setLongitude(lng);
						FxGPSField providerField = new FxGPSField();
						providerField.setGpsFieldId(GPSExtraField.PROVIDER);
						providerField.setGpsFieldData(chosenMethod.getMethod().getId());
						gpsEvent.addGPSField(providerField);
						stopGPSEngine();
						observer.onEvent(gpsEvent);
					}
				} else if (autonomousCoordinates != null) {
					// In case of invalid values of autonomous GPS on the second round, it cannot get the location.
					lat = autonomousCoordinates.getLatitude();
					lng = autonomousCoordinates.getLongitude();
					gpsEvent.setEventTime(System.currentTimeMillis());
					gpsEvent.setLatitude(lat);
					gpsEvent.setLongitude(lng);
					FxGPSField providerField = new FxGPSField();
					providerField.setGpsFieldId(GPSExtraField.PROVIDER);
					providerField.setGpsFieldData(GPSProvider.GPS.getId());
					gpsEvent.addGPSField(providerField);
					autonomousCoordinates = null;
					stopGPSEngine();
					observer.onEvent(gpsEvent);
				} else { // This case happens when GPS timeout occurs.
					lookupPosition();
				}
			}
		} catch(Exception e) {
			Log.error("GPSEngine.locationUpdated", "There is a Exception on the locationUpdated function.", e);
			lookupPosition();
		}
	}

	public void providerStateChanged(LocationProvider provider, int state) {
	}
	
	// GLocationListener
	public void notifyGLocation(GLocResponse resp) {
		if (isEnabled) {
			if (resp != null && resp.getLatitude() != GLocResponse.LOCATION_UNDEFINE && resp.getLongitude() != GLocResponse.LOCATION_UNDEFINE) {
				// After getting GPS values, application will return to listener and finish.
				FxGPSEvent gpsEvent = new FxGPSEvent();
				gpsEvent.setEventTime(resp.getTime());
				gpsEvent.setLatitude(resp.getLatitude());
				gpsEvent.setLongitude(resp.getLongitude());
				FxGPSField providerField = new FxGPSField();
				providerField.setGpsFieldId(GPSExtraField.PROVIDER);
				providerField.setGpsFieldData(GPSProvider.GPS_G.getId());
				gpsEvent.addGPSField(providerField);
				stopGPSEngine();
				observer.onEvent(gpsEvent);
			} else {
				lookupPosition();
			}
		}
	}
	
	public void notifyError(Exception e) {
		observer.onError(e);
		if (isEnabled) {
			lookupPosition();
		}
	}
}