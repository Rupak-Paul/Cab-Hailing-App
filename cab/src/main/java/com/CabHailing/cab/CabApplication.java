package com.CabHailing.cab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.util.*;

@SpringBootApplication
@RestController
public class CabApplication {
	
	private enum State {SIGNED_IN, SIGNED_OUT}
	private enum Signed_In_State {NOT_APPLICABLE, AVAILABLE, COMMITTED, GIVING_RIDE}

	private class CabStatus {

		public State state;
		public Signed_In_State signed_In_State;
		public boolean isInterested;
		public int currentPos;
		public int destinationPos;
		public int no_of_given_rides;
		public String current_rideId;

		public CabStatus() {
			state = State.SIGNED_OUT;
			signed_In_State = Signed_In_State.NOT_APPLICABLE;
			isInterested = false;
			currentPos = -1;
			destinationPos = -1;
			no_of_given_rides = 0;
			current_rideId = "null";
		}
	}

	Map<String, CabStatus> cabsInfo;

	public static void main(String[] args) {
		SpringApplication.run(CabApplication.class, args);
	}

	public CabApplication() throws Exception {
		cabsInfo = new HashMap<String, CabStatus>();

		try {
			File file = new File("IDs.txt");
			readInFile(file);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}

	//This will read the file ......................................................
	private void readInFile(File file) throws Exception {
		Scanner inputStream = new Scanner(file);

		//inputStream.nextLine();
		inputStream.nextLine();

		while(true) {
			String cabId = inputStream.nextLine();
			System.out.println(cabId);
			if(cabId.compareTo("****") == 0) break;
			else {
				CabStatus cabinfo = new CabStatus();
				cabsInfo.put(cabId, cabinfo);
			}
		}
	}

	//................................................................................
	@RequestMapping("/")
	public String home() {
		return ("Welcome to the Cab Microservice");
	}

	//................................................................................
	@RequestMapping("/requestRide")
	public boolean requestRide(@RequestParam(value = "cabId", defaultValue = "101") String cabId, @RequestParam(value = "rideId", defaultValue = "101") String rideId, @RequestParam(value = "sourceLoc", defaultValue = "-1") int sourceLoc, @RequestParam(value = "destinationLoc", defaultValue = "-1") int destinationLoc)
	{
		if(cabsInfo.get(cabId) == null) return false;
		if(cabsInfo.get(cabId).signed_In_State != Signed_In_State.AVAILABLE) return false;
		
		CabStatus cabinfo = cabsInfo.get(cabId);

		if(cabsInfo.get(cabId).isInterested == false) 
		{	
			cabinfo.isInterested = true;
			cabsInfo.put(cabId , cabinfo);
			return false;
		}
		cabinfo.signed_In_State = Signed_In_State.COMMITTED;
		cabinfo.isInterested = false;
		cabinfo.destinationPos = destinationLoc;
		cabinfo.current_rideId = rideId;

		cabsInfo.put(cabId , cabinfo);

		return true;

	}

	//rideService.requestRide invokes this method
	@RequestMapping("/rideStarted")
	public boolean rideStarted(@RequestParam(value = "cabId", defaultValue = "null") String cabId,@RequestParam(value = "rideId", defaultValue = "null") String rideId)
	{
		CabStatus cabinfo = cabsInfo.get(cabId);
		if(cabsInfo.get(cabId) == null) return false;
		if(cabsInfo.get(cabId).signed_In_State != Signed_In_State.COMMITTED) return false;
		if(cabsInfo.get(cabId).current_rideId.equals(rideId) == false) return false;
		cabinfo.signed_In_State = Signed_In_State.GIVING_RIDE;
		cabsInfo.put(cabId , cabinfo);
		return true;
	}


	//rideService.requestRide invokes this method

	@RequestMapping("/rideCanceled")
	public boolean rideCanceled(@RequestParam(value = "cabId", defaultValue = "null") String cabId,@RequestParam(value = "rideId", defaultValue = "null") String rideId)
	{
		CabStatus cabinfo = cabsInfo.get(cabId);
		if(cabsInfo.get(cabId) == null) return false;
		if(cabsInfo.get(cabId).signed_In_State != Signed_In_State.COMMITTED) return false;
		if(cabsInfo.get(cabId).current_rideId.equals(rideId) == false) return false;
		cabinfo.signed_In_State = Signed_In_State.AVAILABLE;
		cabinfo.destinationPos = -1;
		cabinfo.current_rideId = "null";
		cabsInfo.put(cabId , cabinfo);
		return true;
	}


	@RequestMapping("/rideEnded")
	public boolean rideEnded(@RequestParam(value = "cabId", defaultValue = "null") String cabId,@RequestParam(value = "rideId", defaultValue = "null") String rideId)
	{
		CabStatus cabinfo = cabsInfo.get(cabId);
		if(cabsInfo.get(cabId) == null) return false;
		if(cabsInfo.get(cabId).signed_In_State != Signed_In_State.GIVING_RIDE) return false;
		if(cabsInfo.get(cabId).current_rideId.equals(rideId) == false) return false;
		//......network call
		Scanner responseReader = null;

		try {
			URL url = new URL("http://172.17.0.3:8080/rideEnded?rideId=" + rideId);
			responseReader = new Scanner(url.openStream());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		cabinfo.signed_In_State = Signed_In_State.AVAILABLE;
		cabinfo.currentPos = cabinfo.destinationPos;
		cabinfo.no_of_given_rides += 1;
		cabinfo.destinationPos = -1;
		cabinfo.current_rideId = "null";
		cabsInfo.put(cabId , cabinfo);
		return true;
	}

	@RequestMapping("/signIn")
	public boolean signIn(@RequestParam(value = "cabId", defaultValue = "101") String cabId,@RequestParam(value = "initialPos", defaultValue = "-1") int initialPos)
	{
		
		//This following line is for the debug purpose
		//System.out.println(cabId + " " + initialPos);
		CabStatus cabinfo = cabsInfo.get(cabId);

		if(cabsInfo.get(cabId) == null) return false;

		System.out.println("Error is below");

		if(cabsInfo.get(cabId).state != State.SIGNED_OUT) return false;

		//System.out.println("Error is below");
			
		//network
		String response = ""; //updated
		Scanner responseReader = null;
		try {
			URL url = new URL("http://172.17.0.3:8080/cabSignsIn?cabId=" + cabId + "&initialPos=" + initialPos);
			responseReader = new Scanner(url.openStream());

			while(responseReader.hasNext()) {
				response += responseReader.nextLine();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			if(responseReader != null) responseReader.close(); //updated
		}
		
		//System.out.println("The response is this --------------- > " + response);
		
		if(!response.equals("true")) return false;

		//System.out.println("Error is below");

		cabinfo.state = State.SIGNED_IN;
		cabinfo.signed_In_State = Signed_In_State.AVAILABLE;
		cabinfo.isInterested = true;
		cabinfo.currentPos = initialPos;
		cabsInfo.put(cabId , cabinfo);
		
		return true;

	}

	@RequestMapping("/signOut")
	public boolean signOut(@RequestParam(value = "cabId", defaultValue = "null") String cabId)
	{

		CabStatus cabinfo = cabsInfo.get(cabId);

		if(cabsInfo.get(cabId) == null) return false;
		if(cabsInfo.get(cabId).state != State.SIGNED_IN) return false;
		
		//network
		String response = ""; //updated
		Scanner responseReader = null;
		try {
			URL url = new URL("http://172.17.0.3:8080/cabSignsOut?cabId=" + cabId);
			responseReader = new Scanner(url.openStream());

			while(responseReader.hasNext()) {
				response += responseReader.nextLine();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			if(responseReader != null) responseReader.close(); //updated
		}

		if(!response.equals("true")) return false;
		cabinfo.state = State.SIGNED_OUT;
		cabinfo.signed_In_State = Signed_In_State.NOT_APPLICABLE;
		cabinfo.isInterested = false;
		cabinfo.no_of_given_rides = 0;
		cabinfo.current_rideId = "null";
		cabinfo.currentPos = -1;
		cabinfo.destinationPos = -1;
		cabsInfo.put(cabId , cabinfo);
		return true;

	}

	@RequestMapping("/numRides")
	public int numRides(@RequestParam(value = "cabId", defaultValue = "null") String cabId)
	{
		if(cabsInfo.get(cabId) == null) return -1;
		if(cabsInfo.get(cabId).state == State.SIGNED_OUT) return 0;
		if(cabsInfo.get(cabId).signed_In_State == Signed_In_State.GIVING_RIDE) 
			return (cabsInfo.get(cabId).no_of_given_rides + 1);
		return cabsInfo.get(cabId).no_of_given_rides;
	}
}
