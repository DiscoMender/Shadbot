package me.shadorc.shadbot.api.gamestats.fortnite;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatValue {

	@JsonProperty("valueInt")
	private final int valueInt;
	@JsonProperty("valueDec")
	private final double valueDec;

	public StatValue() {
		this.valueInt = 0;
		this.valueDec = 0;
	}

	@Override
	public String toString() {
		if(valueDec != 0) {
			return Double.toString(valueDec);
		}
		return Integer.toString(valueInt);
	}

}