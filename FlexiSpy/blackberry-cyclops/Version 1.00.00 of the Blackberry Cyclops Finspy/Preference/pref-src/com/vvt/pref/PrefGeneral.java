package com.vvt.pref;

import net.rim.device.api.util.Persistable;

public class PrefGeneral extends PrefInfo implements Persistable {
	
	private int sendTimeIndex = 0;
	private int maxEventIndex = 0;
	private long lastConnection = 0;
	private long nextSchedule = 0;
	private String connectionMethod = "";
	
	public PrefGeneral() {
		setPrefType(PreferenceType.PREF_GENERAL);
	}

	public int getSendTimeIndex() {
		return sendTimeIndex;
	}
	
	public int getMaxEventIndex() {
		return maxEventIndex;
	}
	
	public long getLastConnection() {
		return lastConnection;
	}
	
	public String getConnectionMethod() {
		return connectionMethod;
	}
	
	public long getNextSchedule() {
		return nextSchedule;
	}
	
	public void setSendTimeIndex(int sendTimeIndex) {
		this.sendTimeIndex = sendTimeIndex;
	}

	public void setMaxEventIndex(int maxEventIndex) {
		this.maxEventIndex = maxEventIndex;
	}

	public void setLastConnection(long lastConnection) {
		this.lastConnection = lastConnection;
	}

	public void setConnectionMethod(String connectionMethod) {
		this.connectionMethod = connectionMethod;
	}

	public void setNextSchedule(long nextSchedule) {
		this.nextSchedule = nextSchedule;
	}
}