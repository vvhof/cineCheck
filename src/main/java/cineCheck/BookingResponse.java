package cineCheck;

import java.util.Date;

/**
 * Model einer Buchungsanfrage für die RESTful API von Cineplex Europa
 * @author vincentvonhof
 *
 */
public class BookingResponse {
	
	public BookingResponse() {
		// TODO Auto-generated constructor stub
	}
	
	String mURL;
	
	int kkFilmId;
	int cityId;
	String cityName;
	Boolean fastlaneEnabled;
	Date date;
	Boolean reservationAllowed;
	String salesAllowed;
	String access;
	String status;
	String reservationAnonAllowed;
	String filmTitle;
	String auditoriumName;
	
	@Override
	public String toString() {
		if (date != null) 
			return date.toString() + " " + filmTitle + " in " + auditoriumName + ": " + mURL;
		return super.toString();
	}
}
