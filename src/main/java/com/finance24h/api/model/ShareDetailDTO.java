package com.finance24h.api.model;

import com.finance24h.api.helpers.Utilities;

import java.util.Calendar;
import java.util.TimeZone;

public class ShareDetailDTO {
	
	private int id = 0;
	private String symbol;
	private String companyName;
	private float price;
	private float prePercentChange;
	private float basicPrice;
	private String currency = "VND";
	private String floor;
	private long updated;
	private float prevClose;
	private float openPrice;
	private float low;
	private float high;
	private float ceilingPrice;
	private float floorPrice;
	private float yearLow;
	private float yearHigh;
	private float marketValue;
	private float marketCapacity;
	private float volume;
	private long averageVolume;
	private float target;
	private long circulationVolume;
	private long accumulatedVolume;
	private float pe;
	private float eps;
	private float beta;
	private float dividends;
	private String earningDate;
	private int currentHour = Calendar.getInstance(TimeZone.getTimeZone(Utilities.timeZone)).get(Calendar.HOUR_OF_DAY);
	private int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
	private boolean isOpen = currentHour >= 9 && currentHour < 15 & dayOfWeek != 1 & dayOfWeek != 7;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public String getCompanyName() {
		return companyName;
	}
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}
	public float getPrice() {
		return price;
	}
	public void setPrice(float price) {
		if (price == 0) return;
		this.price = price;
	}
	public float getPriceChange() {
		return price - basicPrice;
	}
	public float getPercentChange() {
		return (basicPrice > 0 && currentHour >= 9 ) ? (price - basicPrice) / basicPrice * 100 : 0;
	}
	public float getPrePercentChange() {
		return prePercentChange;
	}
	public void setPrePercentChange(float prePercentChange) {
		this.prePercentChange = prePercentChange;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public String getFloor() {
		return floor;
	}
	public void setFloor(String floor) {
		this.floor = floor;
	}
	public long getUpdated() {
		return updated;
	}
	public void setUpdated(long updated) {
		this.updated = updated;
	}
	public float getPrevClose() {
		return prevClose;
	}
	public void setPrevClose(float prevClose) {
		this.prevClose = prevClose;
	}
	public float getOpenPrice() {
		return openPrice;
	}
	public void setOpenPrice(float openPrice) {
		if (openPrice == 0) return;
		this.openPrice = openPrice;
	}
	public float getLow() {
		return low;
	}
	public void setLow(float low) {
		if (low == 0) return;
		this.low = low;
	}
	public float getHigh() {
		return high;
	}
	public void setHigh(float high) {
		if (high == 0) return;
		this.high = high;
	}
	public float getYearLow() {
		return yearLow;
	}
	public void setYearLow(float yearLow) {
		this.yearLow = yearLow;
	}
	public float getYearHigh() {
		return yearHigh;
	}
	public void setYearHigh(float yearHigh) {
		this.yearHigh = yearHigh;
	}
	public float getMarketCapacity() {
		return marketCapacity;
	}
	public void setMarketCapacity(float marketCapacity) {
		this.marketCapacity = marketCapacity;
	}
	public long getAverageVolume() {
		return averageVolume;
	}
	public void setAverageVolume(long averageVolume) {
		this.averageVolume = averageVolume;
	}
	public float getTarget() {
		return target;
	}
	public void setTarget(float target) {
		this.target = target;
	}
	public long getCirculationVolume() {
		return circulationVolume;
	}
	public void setCirculationVolume(long circulationVolume) {
		this.circulationVolume = circulationVolume;
	}
	public long getAccumulatedVolume() {
		return accumulatedVolume;
	}
	public void setAccumulatedVolume(long accumulatedVolume) {
		this.accumulatedVolume = accumulatedVolume;
	}
	public float getPe() {
		return pe;
	}
	public void setPe(float pe) {
		this.pe = pe;
	}
	public float getEps() {
		return eps;
	}
	public void setEps(float eps) {
		this.eps = eps;
	}
	public float getBeta() {
		return beta;
	}
	public void setBeta(float beta) {
		this.beta = beta;
	}
	public float getDividends() {
		return dividends;
	}
	public void setDividends(float dividends) {
		this.dividends = dividends;
	}
	public String getEarningDate() {
		return earningDate;
	}
	public void setEarningDate(String earningDate) {
		this.earningDate = earningDate;
	}
	public String getSessionStatus() {
		return isOpen ? Utilities.getLabel("session.opened") : Utilities.getLabel("session.closed");
	}
	public boolean isOpen() {
		return isOpen;
	}
	public float getBasicPrice() {
		return basicPrice;
	}
	public void setBasicPrice(float basicPrice) {
		this.basicPrice = basicPrice;
	}
	
	public float getMarketValue() {
		return marketValue;
	}
	public void setMarketValue(float marketValue) {
		this.marketValue = marketValue;
	}
	public float getVolume() {
		return volume;
	}
	public void setVolume(float volume) {
		this.volume = volume;
	}
	public float getCeilingPrice() {
		return ceilingPrice;
	}
	public void setCeilingPrice(float ceilingPrice) {
		this.ceilingPrice = ceilingPrice;
	}
	public float getFloorPrice() {
		return floorPrice;
	}
	public void setFloorPrice(float floorPrice) {
		this.floorPrice = floorPrice;
	}
	
}
