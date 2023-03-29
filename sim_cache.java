import java.io.IOException;
import java.util.Scanner;

public class sim_cache {
	
	public static void main(String[] args) throws IOException {
		//Getting parameters as input from command line
		Scanner myObj = new Scanner(System.in);  // Creating a Scanner object for getting trace file as input
		System.out.println("trace_file:");
		String tf = myObj.nextLine();
		System.out.println("BLOCKSIZE:");
		int a = myObj.nextInt();
		System.out.println("L1_SIZE:");
		int b = myObj.nextInt();
		System.out.println("L1_ASSOC:");
		int c = myObj.nextInt();
		System.out.println("L2_SIZE:");
		int d = myObj.nextInt();
		System.out.println("L2_ASSOC:");
		int e = myObj.nextInt();
		System.out.println("REPLACEMENT POLICY:");
		int f = myObj.nextInt();
		System.out.println("INCLUSION PROPERTY:");
		int g = myObj.nextInt();

		//Calling the CacheMain Class and inserting the values in the respective parameters
		new Main_Caching_System(
				a, b, c, d, e, f, g, tf);

	}

}
