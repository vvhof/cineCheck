package cineCheck;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Handles operations upon cineplex.de booking websites.
 * @author vincentvonhof
 *
 */
public class ChkR {
	
	// Vocabulary used while probing.
	public char[] vocab = {'A','B','C','D','E','F','1','2','3','4','5','6','7','8','9','0'};
	
	private static final String httpPrefix = "https://www.cineplex.de/booking-init/site/";
	private static final String httpCheck   = "cineplex.de/booking-init";
	private static final String httpCheckAlt = "booking.cineplex.de";
	private static final String jsonPrefix = "https://booking.cineplex.de/TicketBoxXNG/booking/init.json?performanceId=";

	private static final String securityChallengeAndResponse = "&c=110dd978-454d-4fc1-9fd9-d88a4d04d06d&r=00340d28223828499d252e984a2be75e8093e13077cb0e60934e22d131d2768d";
	
	// ms to wait between GETs
	private static final int backoff = 20;
	private static int varyingPortionLength = 2;
	
	public ChkR() {}
	
	public ChkR(char[] vocabulary) {
		vocab = vocabulary;
	}
	
	/**
	 * List all Shows
	 * @param url HTTP site of booking for one of the shows for that movie
	 */
	public List<BookingResponse> getAllListingsForMovie(String url) {
		return getAllListingsForMovie(url, varyingPortionLength);
	}
	
	/**
	 * List all Shows
	 * @param url HTTP site of booking for one of the shows for that movie
	 * @param prefixLength amount of varying portion of the shows ID
	 * @throws IOException thrown upon network error
	 */
	public List<BookingResponse> getAllListingsForMovie(String url, int prefixLength) {
		// Check URL validity
		if (!(url.toLowerCase().contains(httpCheck.toLowerCase()) || url.toLowerCase().contains(httpCheckAlt.toLowerCase()))) {
			System.out.println("URL should entail " + httpPrefix);
			return null;
		}
		// Prepare JSON deserializer
		Gson gson = prepareGson();
		
		// Derive site and performance IDs from URL 
		int beginPos = url.indexOf("/site/", 0) + "/site/".length();
		int endPos = beginPos + 2;
		String site = url.substring(beginPos, endPos);
		beginPos = url.indexOf("/performance/", 0) + "/performance/".length();
		endPos = beginPos + 18;
		String performance =  url.substring(beginPos, endPos);
		
		if (site.length() == 0 || performance.length() == 0) {
			System.out.println("Could not derive City or Performance IDs");
			return null;
		}
		
		// Derive postfix
		String postfixPerfID = performance.substring(varyingPortionLength);
		String andSiteID = "&siteId=" + site;
				
		// Get ID from film that interests us from server
		Client client = ClientBuilder.newClient();
		Invocation.Builder invocationBuilder = client
				.target(jsonPrefix + performance + "&siteId=" + site + securityChallengeAndResponse)
				.request(MediaType.APPLICATION_JSON);
		BookingResponse bookingResponse;
		try {
			bookingResponse = gson.fromJson(
					invocationBuilder.get(String.class), BookingResponse.class);
		} catch (javax.ws.rs.InternalServerErrorException ex500) {
			// This URL that we were supplied is pointing a show not recognized by the server. Possibly it's an old listing.
			System.out.println("The server has no entry for the supplied URL. Is the link you provided currently live?");
			return null;
		}
		int kkFilmId = bookingResponse.kkFilmId;

		// Calculate all possible combinations
		Variants variants = new Variants(vocab);
		List<char[]> combinations = variants.generateCombinations(prefixLength);
		System.out.println("Trying " + combinations.size() +" ID combinations");
		
		List<BookingResponse> bookingsInSystem = new ArrayList<BookingResponse>(combinations.size());
		int countHit = 0, countMiss = 0, countWrongMovie = 0;
		for (char[] combination : combinations) {
			invocationBuilder = client
					.target(jsonPrefix + new String(combination) + postfixPerfID + andSiteID + securityChallengeAndResponse)
					.request(MediaType.APPLICATION_JSON);
			// Try our request, if there is no hit we get a HTTP 500 Internal Server Error which we can ignore
			try {
				bookingResponse = gson.fromJson(
						invocationBuilder.get(String.class), BookingResponse.class);
				if (bookingResponse.kkFilmId == kkFilmId) {
					bookingResponse.mURL = httpPrefix + "/site/" + site + "/performance/" + new String(combination) + postfixPerfID;
					bookingsInSystem.add(bookingResponse);
					countHit++;
				} else {
					countWrongMovie++;
				}
			} catch (InternalServerErrorException ex) {
				countMiss++;
			}
			// Print a refreshing short status report to the console
			int total = countHit + countMiss + countWrongMovie;
			String statusText = 
					"Hit: " + countHit +
					" Miss: " + countMiss + 
					" WrongMovie: " + countWrongMovie +
					". Total:" + total + "/" +combinations.size();
			System.out.print(statusText + "\r");	
			// Let's not be too aggressive
			try {Thread.sleep(backoff);} catch (InterruptedException e) {e.printStackTrace();}
		}		
		if (bookingsInSystem.size() == 0) {
			System.out.println("No bookings found for URL");
			return null;
		}
		// Sort results by date
		Collections.sort(bookingsInSystem, new Comparator<BookingResponse>() {
			public int compare(BookingResponse o1, BookingResponse o2) {
				return o1.date.compareTo(o2.date);
			}
		});

		// Print a summary to the console
		for(BookingResponse booking : bookingsInSystem) {
			System.out.println(booking);
		}
		System.out.println("Tried " + countMiss +" not exisiting booking IDs, and " + countWrongMovie + " entries for a different movie");
		
		return bookingsInSystem;
	}
	
	/**
	 * Initialize gson
	 * @return Gson instance for this case
	 */
	private Gson prepareGson() {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.setDateFormat("d.M.yyyy hh:mm");
		return gsonBuilder.serializeNulls().create();
	}
	
	/**
	 * 
	 * @param args <URL> (optional <Output Destination>)
	 */
	public static void main(String[] args) {
		List<BookingResponse> validBookings;
		if (args == null || args.length == 0 || args[0] == null || args[0].length() == 0) {
			System.out.println("Please supply a URL of a Cineplex.de screening listing");
		} else {
			// Get all valid listings
			validBookings = new ChkR().getAllListingsForMovie(args[0]);
			if (validBookings != null && validBookings.size() > 0) {
				PrintWriter writer = null;
				String URI = "BookingsFor" + validBookings.get(0).filmTitle.replace(" ", "").replace(".", "").replace("\\","").replace("/","") + ".txt";
				// If we were supplied a destination, we'll write the results to a file there
				if (args.length >= 2 && args[1] != null && args[1].length() != 0 ) {
					URI = args[1].concat(URI);
				}
				try {
					writer =  new PrintWriter(URI, "UTF-8");
					for (BookingResponse booking : validBookings) {
						writer.println(booking);
					}
					System.out.println("Results written to " + URI);
				}
				catch (IOException ex) {
					System.out.println("Results could not be written to the specified destination");
				} finally {
					   try {if (writer != null) writer.close();} catch (Exception ex) {}
				}
			}
		}
	}

}
