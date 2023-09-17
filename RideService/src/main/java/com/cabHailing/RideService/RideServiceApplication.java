package com.cabHailing.RideService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.*; 
import java.util.*;
import org.springframework.web.client.RestTemplate; 
import java.lang.Math; 

@SpringBootApplication
@RestController
public class RideServiceApplication {

	
	int[][] table = new int[100001][8];//It stores all the values related to the cabs and rides
	int[] rideid = new int[100001];//It stores the ongoing rides and the cab mapping
	int[] cabindex = new int[100001];//It stores the index of the cabs
	int no_of_cabs;//This will store the number of cabs present
	//int[] location = new int[1001];//It stores the location of cabs in avail state
 
	//It reads the cabId which is present in a textfile......................................
	private void readInFile(File file) throws Exception{
	  try {
	  	BufferedReader br = new BufferedReader(new FileReader(file));

		String st; 
		int i = 0;
		st = br.readLine();
		while ((st = br.readLine()) != null) {
			if(st.compareTo("****") == 0) break;
			int num = Integer.parseInt(st);
			table[i][0] = num;
			cabindex[num] = i;//it stores on which index cabId = num is present
			i++;
		}
		
		no_of_cabs = i;

	 } catch (FileNotFoundException e) {
		    e.printStackTrace();
		 }
        }

	public RideServiceApplication() throws Exception{
		
		//This piece of code initializes the arrays and all data structures present...................

		for(int i = 0;i<100000;i++)
		{
			rideid[i] = -1;
			cabindex[i] = -1;
			for(int j = 0;j<8;j++)
			{
				table[i][j] = -1;
			}
		}

		//............................................................................................

		try{
			File file = new File("IDs.txt");
			readInFile(file);
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}
	

	
	@RequestMapping("/")
	public String RideService(){
		return "Welcome to CabHailing Service";
	}
	

	public static void main(String[] args) {
		SpringApplication.run(RideServiceApplication.class, args);
	}

	
	@RequestMapping("/rideEnded")
	public boolean rideEnded(@RequestParam(value = "rideId", defaultValue = "1") int rideId){
		
		//If rideId is not an ongoing ride then return false
		//Else currentpos of cab is the destination loc
		//Now cab state is available
		//remove rideId
		//return true

		if(rideid[rideId] == -1) return false;
		
		//now i know that the rideid is a ongoing ride. which have cabid
		int ind = cabindex[rideid[rideId]];
		table[ind][1] = 0;//Now this cab is in available state
		table[ind][2] = table[ind][5];//last knownposiition becomes the destination
		table[ind][3] = -1;//Now it doesnot have any customer
		table[ind][4] = -1;//updates the rideId
		table[ind][5] = -1;//Destination is also got updated
		rideid[rideId] = -1;
		
		return true;
		
	}
	
	@RequestMapping("/cabSignsIn")
	public boolean cabSignsIn(@RequestParam(value = "cabId", defaultValue = "1") int cabId ,@RequestParam(value = "initialPos", defaultValue = "1")  int initialPos){
	
		//If cabId is invalid or the cab is in signedIn state then return false
		//Else make position of cab to the initialpos
		//and make the cab available
		//return true	

		if(cabindex[cabId] == -1) return false;
		int ind = cabindex[cabId];
		if(table[ind][1] != -1) return false;
		
		table[ind][1] = 0;//Now this cab is in available state
		table[ind][2] = initialPos;//last knownposiition becomes the initialPos
		table[ind][3] = -1;//Now it doesnor have any customer
		table[ind][4] = -1;//updates the rideId
		table[ind][5] = -1;//Destination is also got updated	


		return true;

	}

	@RequestMapping("/cabSignsOut")
	public boolean cabSignsOut(@RequestParam(value = "cabId", defaultValue = "1") int cabId){
		
		//Return true if cabId is valid and the cab is in available state
		//Make appropriate changes
		//Else return false
		
		if(cabindex[cabId] == -1) return false;
		int ind = cabindex[cabId];
		if(table[ind][1] != 0) return false;
		

		table[ind][1] = -1;//Now this cab is in signout state
		table[ind][2] = -1;//last knownposiition
		table[ind][3] = -1;//Now it doesnor have any customer
		table[ind][4] = -1;//updates the rideId
		table[ind][5] = -1;//Destination is also got updated
		
		return true;

	}

	@RequestMapping("/requestRide")
	public int requestRide(@RequestParam(value = "custId", defaultValue = "1") int custId,@RequestParam(value = "sourceLoc", defaultValue = "1")  int sourceLoc,@RequestParam(value = "destinationLoc", defaultValue = "1")  int destinationLoc){
		
		//1. Generate a unique Id
		//2. Request the cabs in the order of increasing distance
		//3. the first time a cab accepts the req calculate the fare
		//4. Then attempt to deduct that amount from the custormes wallet
		// If successful then send cab.rideStarted to the accepting cabId and respond 
		// the customer with the generated Id
		// Else send request cab.rideCancelled to the accepting cabId and then respond customer -1
		//5. Request atmost 3 cabs

		RestTemplate restTemplate = new RestTemplate();
		Random rand = new Random();

		int rideId = rand.nextInt(100000);
		while(rideid[rideId] != -1)
		{
			rideId = rand.nextInt(100000);
		}
	
		//now i got unique rideId
		//now i have to request the cabs inorder of increasing distance
		
		int[] arr = new int[3];

		int first = -1 , firstdist = 1000000;
		int second = -1 , seconddist = 1000000;
		int third = -1 , thirddist = 1000000;

		for(int i = 0;i<no_of_cabs;i++)
		{
			
			if(table[i][1] != 0) continue;

			//now the cab is in available state
			
			//find the distance between the sourceLoc and lastKnown pos
			int dist =  Math.abs(table[i][2] - sourceLoc);
			

			if(dist < firstdist)
			{
				third = second;
				thirddist = seconddist;
				second = first;
				seconddist = firstdist;
				first = table[i][0];
				firstdist = dist;
			}
			else if(dist < seconddist)
			{
				third = second;
				thirddist = seconddist;
				second = table[i][0];
				seconddist = dist;
			}
			else if(dist < thirddist)
			{
				third = table[i][0];
				thirddist = dist;
			}	
		}


		arr[0] = first;
		arr[1] = second;
		arr[2] = third;
		//now i have the cabids to send request to		

		for(int i = 0;i<3;i++)
		{
			
			if(arr[i] == -1) return -1;
			
			

			String my_url = "http://172.17.0.2:8080/requestRide?cabId=" + String.valueOf(arr[i]) + "&rideId=" + String.valueOf(rideId) + "&sourceLoc=" + String.valueOf(sourceLoc) + "&destinationLoc=" + String.valueOf(destinationLoc);
		
			boolean res = restTemplate.getForObject(my_url, boolean.class);

			if(res == true)
			{
				//Now calculate fare
				//As the cab has accepted the request it has gone to the commited state
				//I do not need to change the state of the cab in the database of the rideService reason 					//yourself	

				//Now i have to change the state of the cab to commited state

					
				int ind = cabindex[arr[i]];
				table[ind][1] = 1;
				table[ind][3] = custId;
				table[ind][4] = rideId;
				table[ind][5] = destinationLoc;
				rideid[rideId] = arr[i];

				int fare = 10 * (Math.abs(sourceLoc - table[ind][2]) + Math.abs(destinationLoc - sourceLoc));
				
				RestTemplate restTemplate2 = new RestTemplate();

				String my_url2 = "http://172.17.0.4:8080/deductAmount?custId=" + String.valueOf(custId) + "&amount=" + String.valueOf(fare);
				boolean res2 = restTemplate2.getForObject(my_url2, boolean.class);
				
				if(res2 == true)
				{
					RestTemplate restTemplate3 = new RestTemplate();


					String my_url3 = "http://172.17.0.2:8080/rideStarted?cabId=" + String.valueOf(arr[i]) + "&rideId=" + String.valueOf(rideId);
					boolean res3 = restTemplate3.getForObject(my_url3, boolean.class);

					if(res3 == true)
					{	
						System.out.println("Every thing is going as planned");
						table[ind][2] = sourceLoc;
						table[ind][1] = 2;
						return rideId;

					}
					else
					{
						System.out.println("There is Something fishy");
						return -1;
					}
				}
				else
				{
					//If amount is not got deducted
					
					RestTemplate restTemplate4 = new RestTemplate();

					String my_url4 = "http://172.17.0.2:8080/rideCanceled?cabId=" + String.valueOf(arr[i]) + "&rideId=" + String.valueOf(rideId);
					boolean res4 = restTemplate4.getForObject(my_url4, boolean.class);
					
					if(res4 == true)
					{
						System.out.println("Every thing is going as planned");
						table[ind][1] = 0;
						table[ind][3] = -1;
						table[ind][4] = -1;
						table[ind][5] = -1;
						rideid[rideId] = -1;
						return -1;
					}
					else
					{
						System.out.println("There is Something fishy");
						return -1;
					}

				}
			}

		}

		return -1;//If all 3 of them deny then return this	
	}

	@RequestMapping("/getCabStatus")
	public String getCabStatus(@RequestParam(value = "cabId", defaultValue = "1") int cabId){

		// return the status of the cab in following format
		// (state , lastknownpos , custID , destLoc)

		String str = "";

		if(cabindex[cabId] == -1) return str;
		
		int ind = cabindex[cabId];

		if(table[ind][1] == -1) str+="signed-out";
		else if(table[ind][1] == 0) str+="available";
		else if(table[ind][1] == 1) str+="committed";
		else str+="giving-ride";

		str+= " ";

		str+=String.valueOf(table[ind][2]);
		
		if(table[ind][1] == 2)
		{
			str+= " ";
			str+=String.valueOf(table[ind][3]);
			str+= " ";
			str+=String.valueOf(table[ind][5]);
		}

		return str;
	}
	


	@RequestMapping("/reset")
	public void reset(){
	
		//1. Send cab.rideEnded req to all cabs in giving ride state
		//2. Make appropriate changes
		//3. send SignOut state to all the cabs in signIn state
		RestTemplate restTemplate = new RestTemplate();

		for(int i = 0;i<no_of_cabs;i++)
		{
			
			if(table[i][1] != 2) continue;
			
			//Now the cab is in giving ride state
			//Now i have to send cab.rideEnded a requset to end the ride rightnow

			
			String my_url = "http://172.17.0.2:8080/rideEnded?cabId=" + String.valueOf(table[i][0]) + "&rideId=" + String.valueOf(table[i][4]);

			boolean res = restTemplate.getForObject(my_url, boolean.class);

			if(res == true)
			{
				System.out.println("Everything is going as planned");
			}
			else
			{
				System.out.println("There is something fishy");
			}
		}
	
		//Now send every cab signout request

		for(int i = 0;i<no_of_cabs;i++)
		{
			if(table[i][1] == -1) continue;

			String my_url = "http://172.17.0.2:8080/signOut?cabId=" + String.valueOf(table[i][0]);
			boolean res = restTemplate.getForObject(my_url, boolean.class);

			if(res == true)
			{
				System.out.println("Everything is going as planned");
			}
			else
			{
				System.out.println("There is something fishy");
			}
		}

	}

	@RequestMapping("/test")
	public String test(@RequestParam(value = "cabId", defaultValue = "1") int cabId){
		
		RestTemplate restTemplate = new RestTemplate();
	
		String my_url = "http://172.17.0.3:8080/getCabStatus?cabId=" + String.valueOf(cabId);		

		String res = restTemplate.getForObject(my_url, String.class);
		
		return res;

	}
	
}
