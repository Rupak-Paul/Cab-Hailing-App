# Cab-Hailing-App
> Backend microservices (similar to Ola, Uber, etc.) of a cab hailing application using **Spring Boot** framework.

# Some simplifying assumptions that are followed in this project:
* All locations are on the x-axis. That is, each location can be described by a single (non-negative) integer.
* There is a fixed set of customers and cabs, specified initially in a text file when the application starts.
* Each customer maintains a wallet with the cab company, and all rides are paid from the wallet only.
* The payment for any trip is deducted from the customer’s wallet before the ride starts.
* Cabs can sign-in and sign-out as per the wishes of the drivers. Only a signed-in cab can offer rides.
* A cab stays at the same location after dropping off a customer until it is called somewhere else to start a new ride, or until it signs out for the day.

# Description of APIs:
**1. Cab:**
> Represents the cabs The “major” states that a cab can be in at any given time are signed-out or signed-in. Within the signed-in state, there are further “minor” states, namely, available, committed, and giving-ride. When a cab signs in for the day, it enters the available state. The rest of the state transitions are explained below.

- **boolean requestRide(int cabId, int rideId, int sourceLoc, int destinationLoc)** <br />
RideService.requestRide invokes this request, to request a ride on cabId to go from “sourceLoc” to “destinationLoc”. rideId is a unique ID that identifies this request. The response to this request is true iff the cab is accepting the request. The cab accepts the request iff cabId is a valid ID and cabId is currently in available state and is interested in accepting the request. Once a true response is sent, the cabId enters the committed state, whereas if a false response is sent it remains in available state. In this implementation the “is interested?” question is answered by making each cab accept the first request that it receives after it signs in on any day, and then, considering all requests it receives while it is in available state, and it is disinterested in every alternate request among these requests. This end-point is isolated. That is, while one requestRide request is being handled for a cabId, if another requestRide request comes in for the same cabId it is made to wait until the first request is responded to.

- **boolean rideStarted(int cabId, int rideId)** <br />
This request is triggered by RideService.requestRide. If cabId is valid and if this cab is currently in committed state due to a previously received Cab.requestRide request for the same rideId, then move into giving-ride state and return true, otherwise do not change state and return false.

- **boolean rideCanceled(int cabId, int rideId)** <br />
This request is triggered by RideService.requestRide. If cabId is valid and if this cab is currently in committed state due to a previously received Cab.requestRide request for the same rideId, then enter available state and return true, otherwise do not change state and return false.

- **boolean rideEnded(int cabId, int rideId)** <br />
This request is triggered by the driver when the ongoing ride ends. If cabId is valid and if this cab is currently in giving-ride state due to a previously received Cab.rideStarted request for the same rideId, then enter available state and send request RideService.rideEnded and return true, otherwise do not change state and return false. A ride is always assumed to end at the originally specified destination of the ride.

- **boolean signIn(int cabId, int initialPos)** <br />
Cab driver will send this request, to indicate his/her desire to sign-in with starting location initialPos. If cabId is a valid ID and the cab is currently in signed-out state, then send a request to RideService.cabSignsIn, forward the response from RideService.cabSignsIn back to the driver, and transition to signed-in state iff the response is true. Otherwise, else responds with -1 and do not change state.

- **boolean signOut(int cabId)** <br />
Cab driver will send this request, to indicate his/her desire to sign-out. If cabId is a valid ID and the cab is currently in signed-in state, then send a request to RideService.cabSignsOut, forward the response from RequestRide.cabSignsOut back to the driver, and transition to signed-out state iff the response is true. Otherwise, else respond with -1 and do not change state.

- **int numRides(int cabId)** <br />
To be used mainly for testing purposes. If cabId is invalid, return -1. Otherwise, if cabId is currently signed-in then return number of rides given so far after the last sign-in (including ongoing ride if currently in giving-ride state), else return 0.

<br />

**2. RideService:**
> This service represents the cab-hailing company. The service internally keep its own record of the current states and positions of the cabs.

- **boolean rideEnded(int rideId)** <br />
Cab uses this request, to signal that rideId has ended (at the chosen destination). Return true iff rideId corresponds to an ongoing ride.

- **boolean cabSignsIn(int cabId, int initialPos)** <br />
Cab cabId invokes this to sign-in and notify the company that it wants to start its working day at location “initialPos”. Response is true from the company iff the cabId is a valid one and the cab is not already signed in.

- **boolean cabSignsOut(int cabId)** <br />
Cab uses this to sign out for the day. Response is true (i.e., the sign-out is accepted) iff cabId is valid and the cab is in available state.

- **int requestRide(int custId, int sourceLoc, destinationLoc)** <br />
Customer uses this to request a ride from the service. The cab service first generates a globally unique rideId corresponding to the received request. It then tries to find a cab (using Cab.requestRide) that is willing to accept this ride. It requests cabs that are currently in available state one by one in increasing order of current distance of the cab from sourceLoc. The first time a cab accepts the request, the service calculates the fare (the formula or this is described later) and attempts to deduct the fare from custId’s wallet. If the deduction was a success, send request Cab.rideStarted to the accepting cabId and then respond to the customer with the generated rideId, else send request Cab.rideCanceled to the accepting cabId and then respond with -1 to the customer. If three cabs have been requested and all of them reject, then respond with -1 to the customer. If fewer than three cabs have been contacted and they all reject the requests and there are no more cabs available to request that are currently signed-in and not currently giving a ride, respond with -1 to the customer. The fare for a ride is equal to the distance from the accepting cab’s current location to sourceLoc plus the distance from sourceLoc to destinationLoc, times 10 (i.e., Rs. 10 per unit distance).

- **String getCabStatus(int cabId)** <br />
This end-point is mainly to enable testing. Returns a tuple of strings indicating the current state of the cab (signed-out/available/committed/giving-ride), its last known position, and if currently in a ride then the custId and destination location. The elements of the tuple are separated by single spaces, and the tuple does not have any beginning and ending demarcators. Last known position is the source position of the current ride if the cab is in giving-ride state, is the sign-in location if it has signed in but not given any ride yet, is the destination of the last ride if it is in available state and gave a ride after sign-in, and is -1 if it is in signed-out state.

- **void reset()** <br />
This end-point will be mainly useful during testing. This end-point sends Cab.rideEnded requests to all cabs that are currently in giving-ride state, then send Cab.signOut requests to all cabs that are currently in sign-in state.

<br />

**3. Wallet:**
> This service represents the wallet of customers.

- **int getBalance(int custId)** <br />
returns current wallet balance of custId

- **bool deductAmount(int custId, int amount)** <br />
If custId has balance >= amount, then reduce their balance by “amount” and return true, else return false. This service is used by RideService.requestRide.

- **bool addAmount(custId, int amount)** <be />
Inverse of deductAmount. Both deductAmount and addAmount are processed in an isolated manner for a custId (i.e., different requests for the same custId will not overlap in time).

- **void reset()** <br />
Reset balances of all customers to the “initial” balance as given in the input text file. This end-point is mainly to help enable testing.

# The format of the input file is as follows:
> When the containers start running they will initialize all wallet balances to the initial balance given in the text file, and treats all cabIds as they are in signed-out state. The format of the input file is as follows: 

****  <br />
101   <br />
102   <br />
103   <br />
104   <br />
****  <br />
201   <br />
202   <br />
203   <br />
****  <br />
10000 <br />

The first section contains the cabIDs, the second section contains the custIDs, while the last section contains the initial wallet balance of all customers. The four *’s are the section-begin markers. Number of digits in each ID is considered 1 to 10 digits.
