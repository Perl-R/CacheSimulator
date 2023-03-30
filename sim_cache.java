import java.io.IOException;
import java.util.Scanner;

public class sim_cache {
	
	public static void main(String[] args) throws IOException {
		//Getting parameters as input from command line
		Scanner scanner = new Scanner(System.in);  // Creating a Scanner object for getting trace file as input
		System.out.println("trace_file:");
		String tf = scanner.nextLine();
		System.out.println("BLOCKSIZE:");
		int a = scanner.nextInt();
		System.out.println("L1_SIZE:");
		int b = scanner.nextInt();
		System.out.println("L1_ASSOC:");
		int c = scanner.nextInt();
		System.out.println("L2_SIZE:");
		int d = scanner.nextInt();
		System.out.println("L2_ASSOC:");
		int e = scanner.nextInt();
		/*
		 *  0: LRU
		 *  1: P-LRU
		 *  2: Optimal
		 *  Note: We need to implement FIFO
		 */
		System.out.println("REPLACEMENT POLICY:");
		int f = scanner.nextInt();
		/*
		 * 0: non-inclusive
		 * 1: inclusive
		 */
		System.out.println("INCLUSION PROPERTY:");
		int g = scanner.nextInt();

		scanner.close();

		//Calling the CacheMain Class and inserting the values in the respective parameters
		new Main_Caching_System(
				a, b, c, d, e, f, g, tf);

	}

}
