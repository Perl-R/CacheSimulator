import java.io.IOException;
import java.util.Scanner;

public class sim_cache {

	final static int NUM_ARGS = 8;

	// Example Usage: java sim_cache gcc_trace.txt 16 1024 2 0 0 0 0
	public static void main(String[] args) throws IOException {
		if (args.length >= NUM_ARGS) 
		{
			// Trace File
			String tf = args[0];
			// Block Size
			int block_size = Integer.parseInt(args[1]);
			// L1_SIZE		
			int l1_size = Integer.parseInt(args[2]);
			// L1_ASSOC
			int l1_assoc = Integer.parseInt(args[3]);
			// L2_SIZE
			int l2_size = Integer.parseInt(args[4]);
			// L2_ASSOC
			int l2_assoc = Integer.parseInt(args[5]);

			/*
			*  Replacement Policy
			*  0: LRU
			*  1: FIFO (In Progress)
			*  2: Optimal
			*  3: SHIP (In Progress)
			*  4: Hawkeye (In Progress)
			*  5: MockingJay (In Progress)
			*  6: Custom (LRU + Random)
			*  Note: We need to implement FIFO
			*/
			int replacement_policy = Integer.parseInt(args[6]);

			/*
			*  Inclusion Property
			* 0: non-inclusive
			* 1: inclusive
			*/
			int inclusion_property = Integer.parseInt(args[7]);

			//Calling the CacheMain Class and inserting the values in the respective parameters
			new Main_Caching_System(
				block_size, l1_size, l1_assoc, l2_size, l2_assoc, replacement_policy, inclusion_property, tf);
		}
		else
		{
			System.out.println("Not enough (or too many) command line arguments supplied");
		}
	}
}
