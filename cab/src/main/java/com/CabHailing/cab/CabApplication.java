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

	Dictionary<String, CabStatus> cabsInfo;

	public static void main(String[] args) {
		SpringApplication.run(CabApplication.class, args);
	}

	public CabApplication() throws Exception {
		cabsInfo = new Hashtable<String, CabStatus>();

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
			if(cabId.compareTo("****") == 0) break;
			else {
				CabStatus cabInfo = new CabStatus();
				cabsInfo.put(cabId, cabInfo);
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
	public boolean requestRide(@RequestParam(value = "cabId", defaultValue = "null") String cabId, @RequestParam(value = "rideId", defaultValue = "null") String rideId, @RequestParam(value = "sourceLoc", defaultValue = "-1") int sourceLoc, @RequestParam(value = "destinationLoc", defaultValue = "-1") int destinationLoc)
	{
		if(cabsInfo.get(cabId) == null) return false;
		if(cabsInfo.get(cabId).signed_In_State != Signed_In_State.AVAILABLE) return false;
		if(cabsInfo.get(cabId).isInterested == false) 
		{
			cabsInfo.get(cabId).isInterested = true;
			return false;
		}
		cabsInfo.get(cabId).signed_In_State = Signed_In_State.COMMITTED;
		cabsInfo.get(cabId).isInterested = false;
		cabsInfo.get(cabId).destinationPos = destinationLoc;
		cabsInfo.get(cabId).current_rideId = rideId;
		return true;

	}

	//rideService.requestRide invokes this method
	@RequestMapping("/rideStarted")
	public boolean rideStarted(@RequestParam(value = "cabId", defaultValue = "null") String cabId,@RequestParam(value = "rideId", defaultValue = "null") String rideId)
	{
		if(cabsInfo.get(cabId) == null) return false;
		if(cabsInfo.get(cabId).signed_In_State != Signed_In_State.COMMITTED) return false;
		if(cabsInfo.get(cabId).current_rideId != rideId) return false;
		cabsInfo.get(cabId).signed_In_State = Signed_In_State.GIVING_RIDE;
		return true;
	}


	//rideService.requestRide invokes this method

	@RequestMapping("/rideCanceled")
	public boolean rideCanceled(@RequestParam(value = "cabId", defaultValue = "null") String cabId,@RequestParam(value = "rideId", defaultValue = "null") String rideId)
	{
		if(cabsInfo.get(cabId) == null) return false;
		if(cabsInfo.get(cabId).signed_In_State != Signed_In_State.COMMITTED) return false;
		if(cabsInfo.get(cabId).current_rideId != rideId) return false;
		cabsInfo.get(cabId).signed_In_State = Signed_In_State.AVAILABLE;
		cabsInfo.get(cabId).destinationPos = -1;
		cabsInfo.get(cabId).current_rideId = "null";
		return true;
	}


	@RequestMapping("/rideEnded")
	public boolean rideEnded(@RequestParam(value = "cabId", defaultValue = "null") String cabId,@RequestParam(value = "rideId", defaultValue = "null") String rideId)
	{
		if(cabsInfo.get(cabId) == null) return false;
		if(cabsInfo.get(cabId).signed_In_State != Signed_In_State.GIVING_RIDE) return false;
		if(cabsInfo.get(cabId).current_rideId != rideId) return false;
		//......network call

		try {
			URL url = new URL("http://localhost:8081/rideEnded?rideId=" + rideId);
			url.openConnection().connect();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		cabsInfo.get(cabId).signed_In_State = Signed_In_State.AVAILABLE;
		cabsInfo.get(cabId).currentPos = cabsInfo.get(cabId).destinationPos;
		cabsInfo.get(cabId).no_of_given_rides += 1;
		cabsInfo.get(cabId).destinationPos = -1;
		cabsInfo.get(cabId).current_rideId = "null";
		return true;
	}

	@RequestMapping("/signIn")
	public boolean signIn(@RequestParam(value = "cabId", defaultValue = "null") String cabId,@RequestParam(value = "initialPos", defaultValue = "-1") int initialPos)
	{
		if(cabsInfo.get(cabId) == null) return false;
		if(cabsInfo.get(cabId).state != State.SIGNED_OUT) return false;
			
		//network
		String response = ""; //updated
		Scanner responseReader = null;
		try {
			URL url = new URL("http://localhost:8081/cabSignsIn?cabId=" + cabId + "&initialPos=" + initialPos);
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
		cabsInfo.get(cabId).state = State.SIGNED_IN;
		cabsInfo.get(cabId).signed_In_State = Signed_In_State.AVAILABLE;
		cabsInfo.get(cabId).isInterested = true;
		cabsInfo.get(cabId).currentPos = initialPos;
		return true;

	}

	@RequestMapping("/signOut")
	public boolean signOut(@RequestParam(value = "cabId", defaultValue = "null") String cabId)
	{
		if(cabsInfo.get(cabId) == null) return false;
		if(cabsInfo.get(cabId).state != State.SIGNED_IN) return false;
		
		//network
		String response = ""; //updated
		Scanner responseReader = null;
		try {
			URL url = new URL("http://localhost:8081/cabSignsOut?cabId=" + cabId);
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
		cabsInfo.get(cabId).state = State.SIGNED_OUT;
		cabsInfo.get(cabId).signed_In_State = Signed_In_State.NOT_APPLICABLE;
		cabsInfo.get(cabId).isInterested = false;
		cabsInfo.get(cabId).no_of_given_rides = 0;
		cabsInfo.get(cabId).current_rideId = "null";
		cabsInfo.get(cabId).currentPos = -1;
		cabsInfo.get(cabId).destinationPos = -1;
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