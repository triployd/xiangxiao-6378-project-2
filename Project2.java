import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;

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
	public static int[] vectorClock;
	public static boolean isActive = false;
	public static int totalAppSent = 0;


	public static void main(String[] args){
		if(args.length < 2)
		{
			System.out.println("Please input the node ID and config file");
			return;
		}
		nodeID = args[0];
		config_file = args[1];
		readConfig();
		System.out.println("readConfig done");
		initializeClock();
		decideActive();
		enableServer();
		sleep(1000);
		

	}

	static void readConfig(){
		lineCount = 0;
		System.out.println("Node "+ nodeID + ": Starting to read config file!");
		System.out.println();
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

	static void initializeClock(){
		vectorClock = new int[numberNodes];
		Arrays.fill(vectorClock, 0);
		return;
	}

	static void decideActive(){
		if(Integer.parseInt(nodeID) == 0){
			isActive = true;
			System.out.println("isActive for node "+nodeID+" : "+isActive);
			return;
		}
		Random randomGenerator = new Random();
		int randomValue = randomGenerator.nextInt(10000);
		if(randomValue > 5000) isActive = true;
		System.out.println("isActive for node "+nodeID+" : "+isActive);
		return;
	}



}