package com.ebay.dss.zds.model.schedule;
/** 
* Created by zhouhuang on 2018年10月30日 
*/
public class ScheduleTime {
	
	String jobType;
	
	Integer dayOfWeeks;

	Integer dayOfMonths;
	
	Integer month;
	
	Integer second;
	
	Integer minute;
	
	Integer hour;
	
	Integer year;

	/**
	 * @return the jobType
	 */
	public String getJobType() {
		return jobType;
	}

	/**
	 * @param jobType the jobType to set
	 */
	public void setJobType(String jobType) {
		this.jobType = jobType;
	}

	/**
	 * @return the second
	 */
	public Integer getSecond() {
		return second;
	}

	/**
	 * @param second the second to set
	 */
	public void setSecond(Integer second) {
		this.second = second;
	}

	/**
	 * @return the minute
	 */
	public Integer getMinute() {
		return minute;
	}

	/**
	 * @param minute the minute to set
	 */
	public void setMinute(Integer minute) {
		this.minute = minute;
	}

	/**
	 * @return the hour
	 */
	public Integer getHour() {
		return hour;
	}

	/**
	 * @param hour the hour to set
	 */
	public void setHour(Integer hour) {
		this.hour = hour;
	}
	
	public String toString(){
		return "[JobType: "+this.getJobType()+"]";
	}

	/**
	 * @return the dayOfWeeks
	 */
	public Integer getDayOfWeeks() {
		return dayOfWeeks;
	}

	/**
	 * @param dayOfWeeks the dayOfWeeks to set
	 */
	public void setDayOfWeeks(Integer dayOfWeeks) {
		this.dayOfWeeks = dayOfWeeks;
	}

	/**
	 * @return the dayOfMonths
	 */
	public Integer getDayOfMonths() {
		return dayOfMonths;
	}

	/**
	 * @param dayOfMonths the dayOfMonths to set
	 */
	public void setDayOfMonths(Integer dayOfMonths) {
		this.dayOfMonths = dayOfMonths;
	}

	/**
	 * @return the month
	 */
	public Integer getMonth() {
		return month;
	}

	/**
	 * @param month the month to set
	 */
	public void setMonth(Integer month) {
		this.month = month;
	}

	/**
	 * @return the year
	 */
	public Integer getYear() {
		return year;
	}

	/**
	 * @param year the year to set
	 */
	public void setYear(Integer year) {
		this.year = year;
	}
}
