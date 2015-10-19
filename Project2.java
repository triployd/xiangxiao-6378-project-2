import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.lang.*;
import java.util.concurrent.Semaphore;

public class Project2{
	//field of the Project2
	public static String config_file;
	public static int numberNodes; //record the number of nodes in the system
	public static final String my_net_id = "xxw130730";
	public static String net_id_config;
	public static String nodeID;
	public static int minPerActive;
	public static int maxPerActive;
	public static int minSendDelay;
	public static int snapshotDelay;
	public static int maxNumber;
	public static int lineCount; //record how many effective lines in config file
	public static ArrayList<String> nodeNames = new ArrayList<String>();
	public static ArrayList<String> hostNames = new ArrayList<String>();
	public static ArrayList<String> portNums = new ArrayList<String>();
	public static ArrayList<String> neighborLists = new ArrayList<String>();
	public static ServerSocket serverSock;
	//system states:
	public static volatile int[] vectorClock;
	public static volatile boolean isActive = false;
	public static volatile int totalAppSent = 0;
	public static volatile long lastTimeSent = 0;
	public static volatile int numMessagesToSend; //anywhere from minPerActive to maxPerActive at one time then turn passive
	public static volatile int numSentThisTime = 0;

	public static ArrayList<String> allMyNeighbors;
	public static Socket[] outSocket;
	public static final String UTDSUFFIX = ".utdallas.edu";
	
	public static Semaphore sem = new Semaphore(1);


	public static void main(String[] args){
		if(args.length < 2)
		{
			System.out.println("Please input the node ID and config file");
			return;
		}

		nodeID = args[0];
		config_file = args[1];
		Project2 project2 = new Project2();
		readConfig();
		allMyNeighbors = getAllNeighbors();
		vectorClock = initializeClock();
		isActive = decideActive(); //50% chance to be active
		numMessagesToSend = getNumberOfMsgToSend();
		
		enableServer();
		sleep(4000);

		connectMyNeighbors();
		sleep(1000);

		project2.startSendThread();

		project2.listenSocket();

	}

	void startSendThread(){
		Thread t_Send = new Thread(new SenderWorker());
		t_Send.start();
	}

	static void readConfig(){
		lineCount = 0;
		//System.out.println("Node "+ nodeID + ": Starting to read config file!");
		//System.out.println();
		try(BufferedReader br = new BufferedReader(new FileReader(config_file))){
			String currentLine;
			while ((currentLine = br.readLine()) != null){
				if(currentLine.trim().length() == 0) continue;
				if(currentLine.trim().charAt(0) == '#') continue;
				if(currentLine.trim().charAt(0) != '#' && currentLine.trim().contains("#")){
					currentLine = currentLine.substring(0, currentLine.indexOf('#')); 
				}
				lineCount++;
				currentLine = currentLine.trim().replaceAll("\\s+", " ");
				//Section 1 : six parameters
				if(lineCount == 1){
					System.out.println("Section 1: ");
					System.out.println("Reading six parameters for node " + nodeID);
					String[] parts1 = currentLine.split("\\s+");
					if(parts1.length != 6){
						System.out.println("Error config information in line 1 for node " + nodeID);
						return;
					}else{
						numberNodes = Integer.parseInt(parts1[0]);
						minPerActive = Integer.parseInt(parts1[1]);
						maxPerActive = Integer.parseInt(parts1[2]);
						minSendDelay = Integer.parseInt(parts1[3]);
						snapshotDelay = Integer.parseInt(parts1[4]);
						maxNumber = Integer.parseInt(parts1[5]);
						System.out.println("Six parameters for node " + nodeID + " : ");
						System.out.println(numberNodes+" "+minPerActive+" "+maxPerActive+" "+minSendDelay+" "+snapshotDelay+" "+maxNumber);
						//lineCount++;
						continue;
					}
				}
				//Section 2: listen ports
				//currentLine.contains("dc") can do the trick too
				//lineCount > 1 && lineCount <= numberNodes + 1 && 
				if(lineCount > 1 && lineCount <= numberNodes + 1 && currentLine.contains("dc")){
					String[] parts2 = currentLine.split("\\s+");
					nodeNames.add(parts2[0]);
					hostNames.add(parts2[1]);
					portNums.add(parts2[2]);
					System.out.println("Section 2: ");
					System.out.println("Node: " + parts2[0] + " host: " + parts2[1] + " port: " + parts2[2]);
					//lineCount++;
					System.out.println("lineCount: " + lineCount);
					continue;
				}
				//Section 3: neighbor lists
				if(lineCount > (numberNodes+1) && lineCount <= ((2*numberNodes)+1)){
					neighborLists.add(currentLine);
					System.out.println("Section 3: ");
					System.out.println("lineCount: " + lineCount);
					System.out.println("neighbors: " + currentLine);
					//lineCount++;
					continue;
				} 
				System.out.println("Bad config file with excessive paths or other incorrect information");
			}
		}catch(IOException e){
			System.out.println("readConfig() exceptions ");
			e.printStackTrace();
		}
	}

	static void enableServer(){
		int port = 0;
		try{
			for(int i=0; i<nodeNames.size(); i++){
				if(Integer.valueOf(nodeID) == Integer.valueOf(nodeNames.get(i))){
					port = Integer.parseInt(portNums.get(i));
				}
			}
			serverSock = new ServerSocket(port);
			System.out.println("Node " + nodeID + " listening on port " + port);
		}catch (IOException e){
			System.out.println("Could not listen on port " + port);
			System.exit(-1);
		}
	}

	static void sleep(int milliseconds){
		try {
			Thread.sleep(milliseconds);
		}catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
	}

	static int[] initializeClock(){
		int[] result = new int[numberNodes];
		Arrays.fill(result, 0);
		return result;
	}

	static boolean decideActive(){
		if(Integer.parseInt(nodeID) == 0){
			return true;
		}
		Random randomGenerator = new Random();
		int randomValue = randomGenerator.nextInt(10000);
		if(randomValue > 5000) return true;
		else return false;
	}

	static ArrayList<String> getAllNeighbors(){
		int i = 0;
		int nodeIDInt = Integer.parseInt(nodeID);
		ArrayList<String> result = new ArrayList<String>();
		while(i < neighborLists.size()){
			String line = neighborLists.get(i);
			if(nodeIDInt == i){
				String[] parts1 = line.split("\\s+");
				for(int j=0; j<parts1.length; j++){
					if(!result.contains(parts1[j])){
						result.add(parts1[j]);
					}
				}
			}else{
				if(line.contains(nodeID)){
					if(!result.contains(Integer.toString(i))){
						result.add(Integer.toString(i));
					}
				}
			}
			i++;
		}
		System.out.println("All neighbors for node "+nodeID+":");
		for(int k=0; k<result.size(); k++){
			System.out.println(result.get(k));
		}
		return result;
	}

	static void connectMyNeighbors(){
		outSocket = new Socket[numberNodes];
		int intID = Integer.parseInt(nodeID);
		for(int i=0; i<allMyNeighbors.size(); i++){
			int target = Integer.parseInt(allMyNeighbors.get(i));
			String host = hostNames.get(target)+UTDSUFFIX;
			int port = Integer.parseInt(portNums.get(target));
			tryConnect(host, port, target);
		}
		return;
	}

	static void tryConnect(String host, int port, int target){
		boolean scanning = true;
		int times = 0;
		while(scanning){
			try{
				System.out.println("host and port and target: ");
				System.out.println(host + " " + port + " " + target);
				outSocket[target] = new Socket(host, port);
				scanning = false;
				PrintWriter writer = new PrintWriter(outSocket[target].getOutputStream(), true); 	//boolean autoflush or not?
				writer.println("Hello, I am node "+nodeID);
				//writer.close();
			}catch(IOException ex){
				if(times > 20){
					System.out.println("Connection failed, need to fix some bugs, giving up reconnecting");
					scanning = false;
				}
				System.out.println("Connection failed, reconnecting in 1 seconds");
				times++;
				sleep(1000);
			}
		}
	}

	static void sendAppMessage(){//send events needs to handle semaphore
		Random randomGenerator = new Random();
		int index = randomGenerator.nextInt(allMyNeighbors.size());
		int target = Integer.parseInt(allMyNeighbors.get(index));
		int intID = Integer.parseInt(nodeID);

		vectorClock[intID]++; //need to use a lock to lock it somewhere
		
		String message = "Application Message, Sent time: " + System.currentTimeMillis() + " " + nodeID + "-" + target + " " + Arrays.toString(vectorClock);
		//System.out.println("Message to be sent: " + message);
		try{
			PrintWriter writer = new PrintWriter(outSocket[target].getOutputStream(), true);
			writer.println(message);
			//writer.close();
		}catch(IOException ex){
			System.out.println("Error in sendAppMessage(), unable to send the message, Node "+nodeID);
			ex.printStackTrace();
		}
	}

	void listenSocket(){
		boolean scanning = true;
		while(scanning){
			ClientWorker w;
			try{
				w = new ClientWorker(serverSock.accept());
				Thread t = new Thread(w);
				t.start();
			}catch(IOException e){
				System.out.println("Accept failed in listenSocket(), node "+nodeID+" terminated");
				try {
					serverSock.close();
					Thread.currentThread().interrupt();
					}catch(IOException ex){
						System.out.println("This node has already terminated, nodeID: " + nodeID);
					}
				System.exit(-1);
			}
		}
	}

	class SenderWorker implements Runnable{

		private volatile boolean running = true;

		SenderWorker(){}

		public void run(){

			sleep(5000);

			while(running){
				//at this moment it needs to send app msg to some other neighbor
				long currentTime = System.currentTimeMillis();
				if( isActive && (currentTime - lastTimeSent) > minSendDelay && numSentThisTime < numMessagesToSend && totalAppSent < maxNumber){

					try{
						sem.acquire();
					}catch(InterruptedException ie){
						System.out.println("sem.acquire failed in SenderWorker.run() ");
					}

					sendAppMessage();
					
					lastTimeSent = currentTime;
					totalAppSent++;
					numSentThisTime++;
					if(totalAppSent >= maxNumber || numSentThisTime >= numMessagesToSend){
						isActive = false;
						numSentThisTime = 0;
					}
					if(totalAppSent >= maxNumber){
						running = false;
					}
					sem.release();
				}
				sleep(50);
			}
		}

	}

	class ClientWorker implements Runnable{
		private Socket client;
		private volatile boolean scanning = true;

		//Constructor
		ClientWorker(Socket client) {
			this.client = client;
		}
		
		public void run(){
			String line;
			BufferedReader in = null;
			PrintWriter out = null;
			//boolean scanning = true;
			int intNodeID = Integer.parseInt(nodeID);
			try{
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				out = new PrintWriter(client.getOutputStream(), true);
			}catch (IOException e) {
				System.out.println("in or out failed in run()");
				System.exit(-1);
			}
			
			while(scanning){//receive events need to handle semaphore too
				try{
					line = in.readLine();
					if(line != null){
						System.out.println("Node "+nodeID+" Message received: " + line);
						if(line.contains("Application")){
							try{
								sem.acquire();
							}catch(InterruptedException ie){
								System.out.println("sem.acquire failed in ClientWorker.run() ");
							}
							vectorClock[intNodeID]++;
							int[] timeStampReceived = extractTimeStamp(line);
							if(timeStampReceived.length != vectorClock.length){
								System.out.println("timeStampReceived.length!=vectorClock.length ! Error !");
							}else{
								//get the max timestamp elements! need to use lock too!
								vectorClock = getNewVectorClock(vectorClock, timeStampReceived);
							}
							if((!isActive) && totalAppSent < maxNumber){
								isActive = true;
								numMessagesToSend = getNumberOfMsgToSend();
							}
							sem.release();
						}else if(line.contains("Marker")){

						}
					}
				}catch(IOException e){
					System.out.println("Read failed from ClientWorker-->run()--> while(scanning)-->try{}");
					scanning = false;
					//System.exit(-1);
				}
			}	
		}
	}

	static int[] extractTimeStamp(String original){
		String cut = original.substring(original.indexOf('[')+1, original.indexOf(']'));
		cut = cut.replaceAll("\\s+", "");
		String[] items = cut.split(",");
		int[] result = new int[items.length];
		for (int i = 0; i < items.length; i++) {
    		try {
        		result[i] = Integer.parseInt(items[i]);
    		}catch(NumberFormatException nfe){
    			System.out.println("NumberFormatException in extractTimeStamp()");
    		}
		}
		return result;
	}

	static int[] getNewVectorClock(int[] first, int[] second){//get the max timestamp elements
		int[] result = new int[first.length];
		if(first.length != second.length){
			System.out.println("first.length != second.length ! error !");
		}else{
			for(int i=0; i<first.length; i++){
				result[i] = (first[i]>second[i])? first[i] : second[i];
			}
		}
		return result;
	}

	static int getNumberOfMsgToSend(){
		Random randomGenerator = new Random();
		int result = randomGenerator.nextInt(maxPerActive - minPerActive + 1);
		result += minPerActive;
		return result;
	}

}