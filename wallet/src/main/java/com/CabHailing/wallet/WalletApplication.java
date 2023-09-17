package com.CabHailing.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.*; 
import java.util.*;

@SpringBootApplication
@RestController
public class WalletApplication {


	Dictionary<String, String> arr = new Hashtable<String, String>();


//This will read the file ......................................................

	  private void readInFile(File file) throws Exception{
	  try {
	  	BufferedReader br = new BufferedReader(new FileReader(file));

		String st;
		st = br.readLine();

		while(true){
			st = br.readLine();
			if(st.compareTo("****") == 0) break;
		} 
		
		List<String> customers = new ArrayList<String>();
  
		while ((st = br.readLine()) != null) {
			if(st.compareTo("****") == 0) break;
			customers.add(st);
		}

		st = br.readLine();

		for(String customer:customers)
			arr.put(customer , st); 

	 } catch (FileNotFoundException e) {
		    e.printStackTrace();
		 }
        }

//................................................................................
	
	public WalletApplication() throws Exception{
		
		try{
			File file = new File("IDs.txt");
			readInFile(file);
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}

	@RequestMapping("/")
	public String home() {
		return ("Welcome to the Wallet Microservice");
	}


	public static void main(String[] args) {
		
		SpringApplication.run(WalletApplication.class, args);
	}
	

	@RequestMapping("/addAmount")
	public boolean addAmount(@RequestParam(value = "custId", defaultValue = "1") int custId,@RequestParam(value = "amount", defaultValue = "1") int amount) {

		//Inverse of deductAmount.
		//Both deductAmount and addAmount need to be processed in an
		//isolated manner​ for a custId (i.e., different requests for the same custId
		//should not overlap in time). Again, this isolation requirement matters
		//only in Phase 2.

	
		String st = String.valueOf(custId);	
		String val = arr.get(st);

		if(val == null) return false;
		else {
			int num = Integer.parseInt(val);
			String ne = String.valueOf(num + amount);
			arr.put(st , ne);
			return true;
		}	

	}

	@RequestMapping("/deductAmount")
	public boolean deductAmount(@RequestParam(value = "custId", defaultValue = "1") int custId,@RequestParam(value = "amount", defaultValue = "1") int amount) {

		// If custId has balance >= amount, then reduce their balance by
		//“amount” and return true, else return false. This service is used by
		//RideService.requestRide.

		String st = String.valueOf(custId);	
		String val = arr.get(st);

		if(val == null) return false;
		else {
			int num = Integer.parseInt(val);
			if(num < amount) return false;
			String ne = String.valueOf(num - amount);
			arr.put(st , ne);
			return true;
		}
	
	}

	
	//It will take input from the URL like .../getBalance?custId = 110
	@RequestMapping("/getBalance")
	public int getBalance(@RequestParam(value = "custId", defaultValue = "1") int custId) {
		
		// Return the balance of the customer given
		// If that customer doesnot exists then return -1
		String st = String.valueOf(custId);	
		String val = arr.get(st);

		if(val == null) return -1;
		else {
			int num = Integer.parseInt(val);
			return num; 
		}

	}
	
	@RequestMapping("/reset")
	public void reset() throws Exception{

	//Does reset the balance of the customers based on the textfile that will be available
		try{
			File file = new File("IDs.txt");
			readInFile(file);
		}
		catch (Exception e) {
			System.out.println(e);
		} 
		
	}


}
