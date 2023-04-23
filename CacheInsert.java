import java.util.*;
import java.math.*;
import java.io.*;

public class CacheInsert {
	Map<String, String> SSMap;
	List<String> SData;
	Cache obj_cache;
	int reads_L1, read_Miss_L1, writes_L1, write_Miss_L1, write_backs_L1;
	int reads_L2, read_miss_L2, writes_L2, write_Miss_L2, write_backs_L2, Memory_Collection;
	int eviction_L1 = 0;
	int global_Idx_Optimal = 0;
	ArrayList<Integer> blank_Flag_L1;
	ArrayList<ArrayList<Integer>> blank_Idx_L1;
	int row_Idx = 0;
	Map<Integer, List<Node>> hex = new HashMap<>();
	// Signature History Counter Table
	// for use with SHiP
	Map<String, Integer> SHCT = new HashMap<>();

	// for MockingJay
	Map<String, Integer> RDP = new HashMap<>(); //Hash: Reuse Distance => {Cache Line Hash: Reuse Distance}
	Map<String, Integer> LineTimeStamps = new HashMap<>(); //Hash: TimeStamp => {Set Hash: TimeStamp}
	int INF_RD = 127; // INF_RD
	int TIMESTAMP_BITS  = 8; // Timestamp
	double TEMP_DIFFERENCE = 1.0/16.0;

	//for Hawkeye
	Map<String, Integer> LivenessInterval = new HashMap<>();
	List<Integer> counter;
    List<String> blk;



  final static String[] REPLACEMENT_POLICIES = {
		"LRU", "FIFO", "optimal", "SHIP", "Hawkeye", "MockingJay", "Custom: LRU + Random"};
	
  public CacheInsert(Cache cons_cache, Map<String, String> cons_SSMap, List<String> cons_SData) {
		this.obj_cache = cons_cache;
		this.SSMap = cons_SSMap;
		this.SData = cons_SData;
		reads_L1 = 0;
		read_Miss_L1 =0;
		writes_L1 =0;
		write_Miss_L1 =0;
		write_backs_L1 = 0;
		reads_L2 = 0;
		read_miss_L2 =0;
		writes_L2 =0;
		write_Miss_L2 =0;
		write_backs_L2 = 0;
		blank_Idx_L1 = new ArrayList<ArrayList<Integer>>(obj_cache.size_L1 / obj_cache.blockSize);
		blank_Flag_L1 = new ArrayList<Integer>(obj_cache.size_L1 / obj_cache.blockSize);
		//AC =  new ArrayList<>(obj_cache.blockSize);

		counter= new ArrayList<Integer>();
		blk= new ArrayList<String>();

		
		for (int i = 0; i < obj_cache.size_L1 / obj_cache.blockSize; i++)
		{
			blank_Idx_L1.add(new ArrayList<Integer>());
			blank_Flag_L1.add(0);
		}
		insert_Cache_Data();
	}

	//Get index from L1 of the address
	int getting_idx_L1(String str_idx_L1)
	{
		try {
			return Integer.parseInt(str_idx_L1.substring(obj_cache.tag_L1, obj_cache.tag_L1+ obj_cache.idx_L1),2);
		}
		// Catch for the Fully Associative Case
		catch (Exception e){
			return 0;
		}
	}

	//Get index from L2 of the address
	int getting_idx_L2(String str_idx_L2)
	{
		try{
			return Integer.parseInt(str_idx_L2.substring(obj_cache.tag_L2, obj_cache.tag_L2+ obj_cache.idx_L2),2);
		}
		catch (Exception e){
			return 0;
		}
	}

	String getting_tag_L1(String str_tag_L1)
	{
		return str_tag_L1.substring(0, obj_cache.tag_L1);
	}

	String getting_tag_L2(String str_tag_L2)
	{
		return str_tag_L2.substring(0, obj_cache.tag_L2);
	}

	void insert_Cache_Data() {

		for (int i = 0; i < SData.size(); i++) {
			String ICD_str = SData.get(i);
			String ICD_temp = ICD_str.split(" ")[1];
			int ICD_index = getting_idx_L1(SSMap.get(ICD_temp));
			if(!hex.containsKey(ICD_index))
				hex.put(ICD_index, new ArrayList<>());
			hex.get(ICD_index).add(new Node(ICD_temp,i));
		}

		for (int i = 0; i < SData.size(); i++) 
		{
			String ICD_str = SData.get(i);
			global_Idx_Optimal = i;
			boolean ICD_write_read = ICD_str.split(" ")[0].equals("r");
			ICD_str = ICD_str.split(" ")[1];
			if(ICD_write_read)
				reading_L1(ICD_str, SSMap.get(ICD_str));
			else
				writing_L1(ICD_str, SSMap.get(ICD_str));
		}

		// Calculate Total Memory Traffic
		if(obj_cache.newL2.size() == 0)
		{
			Memory_Collection = read_Miss_L1 + write_Miss_L1 + write_backs_L1;
		}
		else 
		{
			Memory_Collection = read_miss_L2 + write_Miss_L2 + write_backs_L2 + eviction_L1;
		}
		
		print_Cache();
	}
	// Mockingjay Timestamp
	int get_tstamp() {
	  long tStamp = System.currentTimeMillis();
	  int eightBitTimestamp = (int) (tStamp % 256);
	  return eightBitTimestamp;
	}

	//reading L1 cache
	void reading_L1(String rL1_data, String rL1_bits) {
		List<Block_Cache> rL1_list = obj_cache.newL1.get(getting_idx_L1(rL1_bits));
		String rL1_tag = getting_tag_L1(rL1_bits);
		reads_L1++;
		for(Block_Cache bc: rL1_list)
		{
			if(bc.block_cache_tag.equals(rL1_tag)) {
				hit_Read_L1(rL1_tag, rL1_list, bc);
				return;
			}
		}
		row_Idx = getting_idx_L1(rL1_bits);
		read_Miss_L1++;
				
		//if empty cache, include data also lru counter value to be decreased
		if(rL1_list.size()< obj_cache.set_L1 )
		{
			for(Block_Cache cb: rL1_list)
			{
				cb.set_block_Cache_AccessCounter_LRU(cb.get_block_Cache_AccessCounter_LRU()-1);
				cb.set_block_Cache_AccessCounter_OPT(cb.block_Cache_AccessCounter_OPT()+1);
			}
			if(blank_Flag_L1.get(row_Idx) != 0)
			{
				rL1_list.add(blank_Idx_L1.get(row_Idx).remove(0),new Block_Cache(rL1_data, rL1_tag, obj_cache.set_L1 -1 , false));
				blank_Flag_L1.set(row_Idx, blank_Flag_L1.get(row_Idx) - 1);
			}
			else
			{
				int def_RDP = INF_RD + 1;
				int tStamp = get_tstamp();
				if (!RDP.containsKey(rL1_tag)){
					RDP.put(rL1_tag, def_RDP);
				}
				if (!LineTimeStamps.containsKey(rL1_tag)){
					LineTimeStamps.put(rL1_tag, tStamp);
				}

				rL1_list.add(new Block_Cache(rL1_data, rL1_tag, obj_cache.set_L1 -1 , false, (int) (tStamp + def_RDP)));
			}
			if(obj_cache.newL2.size() != 0)
			{
				reading_L2(rL1_data, rL1_bits, false, null);
			}
		}
		else //applying replacement policy
		{
			updating_Cache_L1(rL1_data, rL1_tag, rL1_list, true);
		}
	}

	//Mockingjay
	int increment_timestamp(int input) {
		input++;
		input = input % (1 << TIMESTAMP_BITS);
		return input;
	}
	int time_elapsed (int set_time, int block_time) {
	  if (set_time >= block_time){
		  return set_time - block_time;
	  }
	  set_time += 1 << TIMESTAMP_BITS;
	  return set_time - block_time;
	}

	// for Hawkeye (age allRRIP of blocks when it OPT predicts miss) -until 7 is reached
	void age_all_lines(List<Block_Cache> u_List){
		for(int i=0;i<u_List.size();i++){
			if(u_List.get(i).RRIP<7){
				int x = u_List.get(i).RRIP;
				u_List.get(i).set_RRIP(x);;
		
			}
	
		}
	}
	//OPTgen predictor: iterates through a sequence of access array that contains liveness interval between block accesses
	boolean OPTpredictor(String block){
		//miss = false;
		//hit = true
		boolean OPTp=false;

		if (blk.size()==0){
			blk.add(0,block);
			counter.add(0, 0);
		}
		else{
			//find previous appearences of block
			for(int i=0 ; i<blk.size(); i++){
				//get previous 
				if(blk.get(i)==block){
					for (int j=i;j< blk.size();j++){
						//miss: just appends block to the arrays (no increment of anything)
						if (counter.get(j)>obj_cache.cache_cap){
								OPTp = false;
								blk.add(i, block);
								counter.add(i, 0);
						}
						//hit: increase counters substring's counter by 1
						else{
							for (int k=i;k<blk.size();k++){
								counter.set(k, counter.get(k)+1);
								OPTp = true;
								blk.add(i, block);
								counter.add(i, 0);
							}

						}
					}
					for (int j =i; j<blk.size();j++){
						counter.set(j, counter.get(j)+1);
						
					}
				}
				else {
					blk.add(i, block);
					counter.add(i, 0);
				}

			}
		}


		return OPTp;
	}

	//Hawkeye predictor: predicts weather the block is cache friendly or cache averse
	boolean hpredictor(String block,List<Block_Cache>u_List){
		//boolean variable for prediction 
		// false = cache friendly
		// 1 = cache averse
		boolean hp;
			if(OPTpredictor(block)){
				hp = true;
			}
			else 
				hp = false;
				if(!OPTpredictor(block))
					age_all_lines(u_List);
		return hp;
	}


	int temporal_diff(int init, int sample) {
		if (sample > init) {
			int diff = sample - init;
			diff = (int) (diff * TEMP_DIFFERENCE);
			diff = Math.min(1, diff);
			return Math.min(init + diff, INF_RD);
		} else if (sample < init) {
			int diff = init - sample;
			diff = (int) (diff * TEMP_DIFFERENCE);
			diff = Math.min(1, diff);
			return Math.max(init - diff, 0);
		} else {
			return init;
		}
	}

	//hit in read in L1
	void hit_Read_L1(String hRL1_tag, List<Block_Cache> hRL1_list, Block_Cache hRL1_c) {
		//lru
		int hRL1_val = hRL1_c.get_block_Cache_AccessCounter_LRU();
		
		//Mockingjay
		if (obj_cache.replacementPolicy == 5){
			int line_RDP = RDP.get(hRL1_tag);
			int last_timestamp = LineTimeStamps.get(hRL1_tag);
			int curr_timestamp = get_tstamp();
			int time_elapsed = time_elapsed(last_timestamp, curr_timestamp); //edit curr_timestamp ?
			if (time_elapsed > INF_RD){
				RDP.put(hRL1_tag, temporal_diff(line_RDP, last_timestamp));
				hRL1_c.set_eta( (int) (curr_timestamp + temporal_diff(line_RDP, last_timestamp)));
			} else {
				RDP.put(hRL1_tag, time_elapsed);
				hRL1_c.set_eta( (int) (curr_timestamp + time_elapsed));
			}
			LineTimeStamps.put(hRL1_tag, curr_timestamp);
		}
		// if using SHiP
		if (obj_cache.replacementPolicy == 3) {
			hRL1_c.set_outcome(true);
			// if the signature of this cache isn't in the cache add it
			if (!SHCT.containsKey(hRL1_c.signature_m))
				SHCT.put(hRL1_c.signature_m, 0);
			
			// increment SHCT[signature_m]
			SHCT.replace(hRL1_c.signature_m, SHCT.get(hRL1_c.signature_m) + 1);			
		}
		
		for(Block_Cache bc: hRL1_list)
		{
			if(bc.block_cache_tag.equals(hRL1_tag)) {
				bc.set_block_Cache_AccessCounter_LRU(obj_cache.set_L1-1);
			}
			else if(bc.get_block_Cache_AccessCounter_LRU() > hRL1_val)
			{
				bc.set_block_Cache_AccessCounter_LRU(bc.get_block_Cache_AccessCounter_LRU()-1);
			}
		}
	}

	//writing in L1
	void writing_L1(String wL1_data, String wL1_bits) {
		List<Block_Cache> wL1_list = obj_cache.newL1.get(getting_idx_L1(wL1_bits));
		String wL1_tag = getting_tag_L1(wL1_bits);
		writes_L1++;
		for(Block_Cache bc: wL1_list)
		{
			if(bc.block_cache_tag.equals(wL1_tag)) {
				hit_Write_L1(wL1_tag, wL1_list, bc);
				bc.set_block_cache_dirtyBit(true);
				return;
			}
		}


		row_Idx = getting_idx_L1(wL1_bits);
		write_Miss_L1++;
		//if empty cache, include data also lru counter value to be decreased
		if(wL1_list.size()< obj_cache.set_L1)
		{
			for(Block_Cache cb: wL1_list)
			{
				cb.set_block_Cache_AccessCounter_LRU(cb.get_block_Cache_AccessCounter_LRU()-1);
				cb.set_block_Cache_AccessCounter_OPT(cb.block_Cache_AccessCounter_OPT()+1);
			}
			if(blank_Flag_L1.get(row_Idx) != 0)
			{
				wL1_list.add(blank_Idx_L1.get(row_Idx).remove(0),new Block_Cache(wL1_data, wL1_tag, obj_cache.set_L1 -1 , true));
				blank_Flag_L1.set(row_Idx, blank_Flag_L1.get(row_Idx) - 1);
			}
			else
			{
				int def_RDP = INF_RD + 1;
				int tStamp = get_tstamp();
				if (!RDP.containsKey(wL1_tag)){
					RDP.put(wL1_tag, def_RDP);
				}
				if (!LineTimeStamps.containsKey(wL1_tag)){
					LineTimeStamps.put(wL1_tag, tStamp);
				}

				wL1_list.add(new Block_Cache(wL1_data, wL1_tag, obj_cache.set_L1 -1 , true, (int) (tStamp + def_RDP)));
			}
			if(obj_cache.newL2.size() != 0)
			{
				reading_L2(wL1_data, wL1_bits, false, null);
			}
		}
		else //applying replacement policy
		{
			updating_Cache_L1(wL1_data, wL1_tag, wL1_list, false);
		}
	}

	//hit write in L1
	void hit_Write_L1(String hWL1_tag, List<Block_Cache> hWL1_list, Block_Cache hWL1_c) {

		int hWL1_val = hWL1_c.get_block_Cache_AccessCounter_LRU();

		//Mockingjay
		if (obj_cache.replacementPolicy == 5){
			int line_RDP = RDP.get(hWL1_tag);
			int last_timestamp = LineTimeStamps.get(hWL1_tag);
			int curr_timestamp = get_tstamp();
			int time_elapsed = time_elapsed(last_timestamp, curr_timestamp); //edit curr_timestamp ?
			if (time_elapsed > INF_RD){
				RDP.put(hWL1_tag, temporal_diff(line_RDP, last_timestamp));
				hWL1_c.set_eta( (int) (curr_timestamp + temporal_diff(line_RDP, last_timestamp)));
			} else {
				RDP.put(hWL1_tag, time_elapsed);
				hWL1_c.set_eta( (int) (curr_timestamp + time_elapsed));
			}
			LineTimeStamps.put(hWL1_tag, curr_timestamp);
		}

		// if using SHiP
		if (obj_cache.replacementPolicy == 3) {
			hWL1_c.set_outcome(true);
			// if the signature of this cache isn't in the cache add it
			if (!SHCT.containsKey(hWL1_c.signature_m))
				SHCT.put(hWL1_c.signature_m, 0);
			
			// increment SHCT[signature_m]
			SHCT.replace(hWL1_c.signature_m, SHCT.get(hWL1_c.signature_m) + 1);			
		}

		
		for(Block_Cache bc: hWL1_list)
		{
			if(bc.block_cache_tag.equals(hWL1_tag)) {

				bc.set_block_Cache_AccessCounter_LRU(obj_cache.set_L1-1);
			}
			else if(bc.get_block_Cache_AccessCounter_LRU() > hWL1_val)
			{
				bc.set_block_Cache_AccessCounter_LRU(bc.get_block_Cache_AccessCounter_LRU()-1);
			}
		}
	}

	//reading in L2
	void reading_L2(String rL1_data, String rL1_bits, boolean rL1_Evict, Block_Cache rL1_block_evicted) {

		List<Block_Cache> rL1_list = obj_cache.newL2.get(getting_idx_L2(rL1_bits));
		String rL1_tag = getting_tag_L2(rL1_bits);

		if(rL1_Evict)
		{
			writing_L2(rL1_block_evicted.get_block_cache_data(),SSMap.get(rL1_block_evicted.get_block_cache_data()));
		}
		reads_L2++;
		for(Block_Cache bc: rL1_list)
		{
			if(bc.get_block_cache_Tag().equals(rL1_tag)) {
				hit_Read_L2(rL1_tag, rL1_list, bc);
				return;
			}
		}
		read_miss_L2++;
		row_Idx = getting_idx_L1(rL1_bits);
		//if empty cache, include data also lru counter value to be decreased
		if(rL1_list.size()< obj_cache.set_L2)
		{
			for(Block_Cache bc: rL1_list)
			{
				bc.set_block_Cache_AccessCounter_LRU(bc.get_block_Cache_AccessCounter_LRU()-1);
				bc.set_block_Cache_AccessCounter_OPT(bc.block_Cache_AccessCounter_OPT()+1);
			}

			int def_RDP = INF_RD + 1;
			int tStamp = get_tstamp();
			if (!RDP.containsKey(rL1_tag)){
				RDP.put(rL1_tag, def_RDP);
			}
			if (!LineTimeStamps.containsKey(rL1_tag)){
				LineTimeStamps.put(rL1_tag, tStamp);
			}

			rL1_list.add(new Block_Cache(rL1_data, rL1_tag, obj_cache.set_L2 -1 , false, (int) (tStamp + def_RDP)));

		}
		else //using replacement policy
		{
			u_Cache_L2(rL1_data, rL1_tag, rL1_list, true);
		}
	}

	//hit read in L2
	void hit_Read_L2(String hRL2_tag, List<Block_Cache> hRL2_list, Block_Cache hRL2_c) {
		int hRL2_val = hRL2_c.get_block_Cache_AccessCounter_LRU();
		
		if (obj_cache.replacementPolicy == 5){
			int line_RDP = RDP.get(hRL2_tag);
			int last_timestamp = LineTimeStamps.get(hRL2_tag);
			int curr_timestamp = get_tstamp();
			int time_elapsed = time_elapsed(last_timestamp, curr_timestamp); //edit curr_timestamp ?
			if (time_elapsed > INF_RD){
				RDP.put(hRL2_tag, temporal_diff(line_RDP, last_timestamp));
				hRL2_c.set_eta( (int) (curr_timestamp + temporal_diff(line_RDP, last_timestamp)));
			} else {
				RDP.put(hRL2_tag, time_elapsed);
				hRL2_c.set_eta( (int) (curr_timestamp + time_elapsed));
			}
			LineTimeStamps.put(hRL2_tag, curr_timestamp);
		}

		// if using SHiP
		if (obj_cache.replacementPolicy == 3) {
			hRL2_c.set_outcome(true);
			// if the signature of this cache isn't in the cache add it
			if (!SHCT.containsKey(hRL2_c.signature_m))
				SHCT.put(hRL2_c.signature_m, 0);
			
			// increment SHCT[signature_m]
			SHCT.replace(hRL2_c.signature_m, SHCT.get(hRL2_c.signature_m) + 1);			
		}
		
		for(Block_Cache bc: hRL2_list)
		{
			if(bc.block_cache_tag.equals(hRL2_tag)) {
				bc.set_block_Cache_AccessCounter_LRU(obj_cache.set_L2-1);
			}
			else if(bc.get_block_Cache_AccessCounter_LRU() > hRL2_val)
			{
				bc.set_block_Cache_AccessCounter_LRU(bc.get_block_Cache_AccessCounter_LRU()-1);
			}
		}
	}

	//writing in L2
	void writing_L2(String wL2_data, String wL2_bits) {
		List<Block_Cache> wL2_list = obj_cache.newL2.get(getting_idx_L2(wL2_bits));
		String wL2_tag = getting_tag_L2(wL2_bits);
		writes_L2++;
		for(Block_Cache bc: wL2_list)
		{
			if(bc.get_block_cache_Tag().equals(wL2_tag)) {
				hit_Write_L2(wL2_tag, wL2_list, bc);
				bc.set_block_cache_dirtyBit(true);
				return;
			}
		}
		write_Miss_L2++;
		row_Idx = getting_idx_L1(wL2_bits);
		//if empty cache, include data also lru counter value to be decreased
		if(wL2_list.size()< obj_cache.set_L2)
		{
			for(Block_Cache bc: wL2_list)
			{
				bc.set_block_Cache_AccessCounter_LRU(bc.get_block_Cache_AccessCounter_LRU()-1);
				bc.set_block_Cache_AccessCounter_OPT(bc.block_Cache_AccessCounter_OPT()+1);
			}

			int def_RDP = INF_RD + 1;
			int tStamp = get_tstamp();
			if (!RDP.containsKey(wL2_tag)){
				RDP.put(wL2_tag, def_RDP);
			}
			if (!LineTimeStamps.containsKey(wL2_tag)){
				LineTimeStamps.put(wL2_tag, tStamp);
			}

			wL2_list.add(new Block_Cache(wL2_data, wL2_tag, obj_cache.set_L2 -1 , true, (int) (tStamp + def_RDP)));
		}
		else //applying replacement policy
		{
			u_Cache_L2(wL2_data, wL2_tag, wL2_list, false);
		}
	}

	//hit write in L2
	void hit_Write_L2(String hWL2_tag, List<Block_Cache> hWL2_list, Block_Cache hWL2_c) {
		int hWL2_val = hWL2_c.get_block_Cache_AccessCounter_LRU();
		
		//Mockingjay
		if (obj_cache.replacementPolicy == 5){
			int line_RDP = RDP.get(hWL2_tag);
			int last_timestamp = LineTimeStamps.get(hWL2_tag);
			int curr_timestamp = get_tstamp();
			int time_elapsed = time_elapsed(last_timestamp, curr_timestamp); //edit curr_timestamp increment ?
			if (time_elapsed > INF_RD){
				RDP.put(hWL2_tag, temporal_diff(line_RDP, last_timestamp));
				hWL2_c.set_eta( (int) (curr_timestamp + temporal_diff(line_RDP, last_timestamp)));
			} else {
				RDP.put(hWL2_tag, time_elapsed);
				hWL2_c.set_eta( (int) (curr_timestamp + time_elapsed));
			}
			LineTimeStamps.put(hWL2_tag, curr_timestamp);
		}

		// if using SHiP
		if (obj_cache.replacementPolicy == 3) {
			hWL2_c.set_outcome(true);
			// if the signature of this cache isn't in the cache add it
			if (!SHCT.containsKey(hWL2_c.signature_m))
				SHCT.put(hWL2_c.signature_m, 0);
			
			// increment SHCT[signature_m]
			SHCT.replace(hWL2_c.signature_m, SHCT.get(hWL2_c.signature_m) + 1);			
		}
		
		for(Block_Cache bc: hWL2_list)
		{
			if(bc.block_cache_tag.equals(hWL2_tag)) {

				bc.set_block_Cache_AccessCounter_LRU(obj_cache.set_L2-1);
			}
			else if(bc.get_block_Cache_AccessCounter_LRU() > hWL2_val)
			{
				bc.set_block_Cache_AccessCounter_LRU(bc.get_block_Cache_AccessCounter_LRU()-1);
			}
		}
	}

	//L1 replacement policy
	void updating_Cache_L1(String u_data, String u_tag, List<Block_Cache> u_list, boolean u_read) {
		int uCL1_idx = 0;
		switch(obj_cache.replacementPolicy)
		{
			// FIFO
			case 1:{
				int target_index = 0;
				int first_in = Integer.MAX_VALUE;
				// Loop to find the first in
				for (int i = 0; i < u_list.size(); i++)
				{
					if (u_list.get(i).fifo_position < first_in) {
						first_in = u_list.get(i).fifo_position;
						target_index = i;
					}
				}
				uCL1_idx = target_index;
				break;
			}
			case 2:{

				uCL1_idx = getting_eviction_idx_opt(u_list);
				int value = u_list.get(uCL1_idx).block_Cache_AccessCounter_OPT();
				for(Block_Cache cb: u_list)
				{
					if(cb.block_Cache_AccessCounter_OPT()<value)
						cb.set_block_Cache_AccessCounter_OPT(cb.block_Cache_AccessCounter_OPT()+1);
				}
				break;
			}
			//Hawkeye
			case 4:{
				for (int i = 0; i< u_list.size(); i++){
					int max=0;
					//if there are any cache averse blocks(==7), set them as target
					if (u_list.get(i).RRIP == 7){
						uCL1_idx= i;
					}
					//if no cache averse blocks, set highest RRIP as target
					else if (u_list.get(i).RRIP > max){
							max = u_list.get(i).RRIP;
							uCL1_idx = i;
					}
	
				}
	
				break;
			}
			/*
			 * Custom LRU + Random Replacement Case
			 * When LRU Misses Exceed a Threshold, we resort to Random Replacement
			 */
			// MockingJay
			case 5:{
				int max_eta = 0;
				for (int i = 0; i < u_list.size(); i++)
				{
					Block_Cache cb = u_list.get(i);
					int etaVal = cb.get_eta();
					if(etaVal > max_eta)
					{
						uCL1_idx = i;
						max_eta = etaVal;
					}
				}
			}
			case 6:{
				int min_misses = obj_cache.size_L1;
				double miss_threshold = 0.2;
	
				// If we exceed this threshold we will random replace
				if (read_Miss_L1 + write_Miss_L1 >= min_misses
					&& getting_MissRate_L1() >= miss_threshold)
				{
					Random rand = new Random();
					int upperbound = u_list.size();					
					int rand_index = rand.nextInt(upperbound);
					uCL1_idx = rand_index;
				}
				// Otherwise we perform standard LRU
				else 
				{
					for (int i = 0; i < u_list.size(); i++)
					{
						Block_Cache cb = u_list.get(i);
						if(cb.get_block_Cache_AccessCounter_LRU() <= 0)
						{
							uCL1_idx = i;
						}
						else
						{
							cb.set_block_Cache_AccessCounter_LRU(cb.get_block_Cache_AccessCounter_LRU()-1);
						}
					}
				}
				break;
			}
			// Perform LRU Replacement (0) by default
      // SHiP uses LRU as a base so SHiP should also map to this part of the switch statement
      default:{
				for (int i = 0; i < u_list.size(); i++)
				{
					Block_Cache cb = u_list.get(i);
					if(cb.get_block_Cache_AccessCounter_LRU() == 0)
					{
						uCL1_idx = i;
					}
					else
					{
						cb.set_block_Cache_AccessCounter_LRU(cb.get_block_Cache_AccessCounter_LRU()-1);
					}
				}
				break;
			}
		}
		Block_Cache evicted = u_list.remove(uCL1_idx);
				
		if(evicted.is_block_cache_dirtyBit())
		{
			write_backs_L1++;
		}
		if (obj_cache.replacementPolicy == 5){

			int def_RDP = INF_RD + 1;
			int tStamp = get_tstamp();
			if (!RDP.containsKey(u_tag)){
				RDP.put(u_tag, def_RDP);
			}
			LineTimeStamps.put(u_tag, tStamp);

			u_list.add(uCL1_idx, new Block_Cache(u_data, u_tag, obj_cache.set_L1 -1 , true, (int) (tStamp + def_RDP) ));

		} else {
			u_list.add(uCL1_idx, new Block_Cache(u_data, u_tag, obj_cache.set_L1 -1 , true));
		}
		if(u_read)
		{
			u_list.get(uCL1_idx).set_block_cache_dirtyBit(false);
		}
		if(obj_cache.newL2.size() != 0 )
		{
			if(evicted.is_block_cache_dirtyBit())
				writing_L2(evicted.get_block_cache_data(), SSMap.get(evicted.get_block_cache_data()));

			reading_L2(u_data, SSMap.get(u_data), false, null);
		}
		
		// if using SHiP
		if (obj_cache.replacementPolicy == 3) {
			// Initialize SHCT[evicted.signature_m] if you haven't already
			if (!SHCT.containsKey(evicted.signature_m)) {
				SHCT.put(evicted.signature_m, 0);
			}
			
			// if evicted.outcome == false
			if (!evicted.get_outcome()) {
				// decrement SHCT[signature_m]
				SHCT.put(evicted.signature_m, SHCT.get(evicted.signature_m) - 1);
			}
			Block_Cache newCache = u_list.get(uCL1_idx);
			
			newCache.set_outcome(false);
			
			// Initialize SHCT[newCache.signature_m] if you haven't already
			if (!SHCT.containsKey(newCache.signature_m)) {
				SHCT.put(newCache.signature_m, 0);
			}
			
			if (SHCT.get(newCache.signature_m) == 0) {
				// predict distant re-reference
				// For LRU this means make the counter 0 and increment all other LRU counters
				for (Block_Cache cb : u_list) {
					cb.set_block_Cache_AccessCounter_OPT(cb.get_block_Cache_AccessCounter_LRU() + 1);
				}
				newCache.set_block_Cache_AccessCounter_LRU(0);
				u_list.set(uCL1_idx, newCache);
			}
			else {
				// predict intermediate re-reference
				// for LRU this means proceed as normal
			}
		}
		
		//Hawkeye
		else if(obj_cache.replacementPolicy ==4){
						//remove all instances (detrianing sampler)
						for (int i =0; i< blk.size();i++){
							if (blk.get(i) == u_list.get(uCL1_idx).block_cache_data)){
								blk.remove(i);
								counter.remove(i);
							}
						}
						//change block from cache
						Block_Cache newCache = u_list.get(uCL1_idx);
						u_list.set(uCL1_idx, newCache);
		}
	}

	int getting_eviction_idx_opt(List<Block_Cache> gvopt_li) {
		int evopt_idx = 0;
		int[] arr = new int[gvopt_li.size()];
		Arrays.fill(arr, Integer.MAX_VALUE);
		List<Node> gvopt_nodeList = hex.get(row_Idx);
		int z1 = 0;
		while(z1 < arr.length){
			Block_Cache bc = gvopt_li.get(z1);
			int z2 = 0;
			while(z2 < gvopt_nodeList.size()){
				Node node = gvopt_nodeList.get(z2);
				if (node.getNode_index() > global_Idx_Optimal) {
					String tag = getting_tag_L1(SSMap.get(node.getNode_str()));
					if (bc.get_block_cache_Tag().equals(tag)) {
						arr[z1] = node.getNode_index() - global_Idx_Optimal;
						break;
					}
				}
				z2++;
			}
			z1++;
		}

		int max = -1;
		for (int i = 0; i < arr.length; i++) 
		{
			max = Math.max(arr[i], max);
		}

		int leastUsed = -1;
		for (int i = 0; i < gvopt_li.size(); i++) 
		{
			if(arr[i] == max && leastUsed < gvopt_li.get(i).block_Cache_AccessCounter_OPT())
			{
				leastUsed = gvopt_li.get(i).block_Cache_AccessCounter_OPT();
				evopt_idx = i;
				return evopt_idx;
			}
		}

		return evopt_idx;
	}

	//L2 replacement policy
	void u_Cache_L2(String UCL2_data, String UCL2_tag, List<Block_Cache> UCL2_list, boolean UCL2_read) {
		int idx = 0;
		// FIFO
		if (obj_cache.replacementPolicy == 1)
		{
			int target_index = 0;
			int first_in = Integer.MAX_VALUE;
			// Loop to find the first in
			for (int i = 0; i < UCL2_list.size(); i++)
			{
				if (UCL2_list.get(i).fifo_position < first_in) {
					first_in = UCL2_list.get(i).fifo_position;
					target_index = i;
				}
			}
			idx = target_index;
		}
		else if (obj_cache.replacementPolicy == 2) {
			idx = getting_eviction_idx_opt(UCL2_list);
			int val = UCL2_list.get(idx).block_Cache_AccessCounter_OPT();
			for (Block_Cache cb : UCL2_list) {
				if (cb.block_Cache_AccessCounter_OPT() < val)
					cb.set_block_Cache_AccessCounter_OPT(cb.block_Cache_AccessCounter_OPT() + 1);
			}
		}

		//Hawkeye
		else if (obj_cache.replacementPolicy == 4){
			for (int i = 0; i< UCL2_list.size(); i++){
				int max=0;
				//if there are any cache averse blocks(==7), set them as target
				if (UCL2_list.get(i).RRIP == 7){
					idx= i;
				}
				//if no cache averse blocks, set highest RRIP as target
				else if (UCL2_list.get(i).RRIP > max){
						max = UCL2_list.get(i).RRIP;
						idx = i;
				}

			}

		}
		/*
		* Custom LRU + Random Replacement Case
		*/
		else if (obj_cache.replacementPolicy == 6) {
			int min_misses = obj_cache.size_L2;
			double miss_threshold = 0.15;

			// If we exceed this threshold we will random replace
			if (read_miss_L2 + write_Miss_L2 >= min_misses
				&& getting_MissRate_L2() >= miss_threshold)
			{
				Random rand = new Random();
				int upperbound = UCL2_list.size();					
				int rand_index = rand.nextInt(upperbound);
				idx = rand_index;
			}
			else if (obj_cache.replacementPolicy == 5) {
				int max_eta = 0;
				for (int i = 0; i < UCL2_list.size(); i++)
				{
					Block_Cache cb = UCL2_list.get(i);
					int etaVal = cb.get_eta();
					if(etaVal > max_eta)
					{
						idx = i;
						max_eta = etaVal;
					}
				}
			}
			// Otherwise we perform standard LRU
			else 
			{
				for (int i = 0; i < UCL2_list.size(); i++)
				{
					Block_Cache cb = UCL2_list.get(i);
					if(cb.get_block_Cache_AccessCounter_LRU() <= 0)
					{
						idx = i;
					}
					else
					{
						cb.set_block_Cache_AccessCounter_LRU(cb.get_block_Cache_AccessCounter_LRU()-1);
					}
				}
			}
		}
		// Case for LRU
		else
		{
			for (int i = 0; i < UCL2_list.size(); i++) 
			{
				Block_Cache cb = UCL2_list.get(i);
				if(cb.get_block_Cache_AccessCounter_LRU() == 0)
				{
					idx = i;
				}
				else
				{
					cb.set_block_Cache_AccessCounter_LRU(cb.get_block_Cache_AccessCounter_LRU()-1);
				}
			}
		}

		// Evict the selected index
		Block_Cache got_evict = UCL2_list.remove(idx);
		if(got_evict.is_block_cache_dirtyBit())
		{
			write_backs_L2++;
		}
		if (obj_cache.replacementPolicy == 5){
			int def_RDP = INF_RD + 1;
			int tStamp = get_tstamp();
			if (!RDP.containsKey(UCL2_tag)){
				RDP.put(UCL2_tag, def_RDP);
			}
			LineTimeStamps.put(UCL2_tag, tStamp);

			UCL2_list.add(idx, new Block_Cache(UCL2_data, UCL2_tag, obj_cache.set_L2 -1 , true, (int) (tStamp + def_RDP) ));
		}
		else {
			UCL2_list.add(idx, new Block_Cache(UCL2_data, UCL2_tag, obj_cache.set_L2 -1 , true));
		}
		if(UCL2_read)
		{
			UCL2_list.get(idx).set_block_cache_dirtyBit(false);
		}

		if(obj_cache.inclusionProperty == 1)
		{
			evict_L1(got_evict);
		}

    	// if using SHiP
		if (obj_cache.replacementPolicy == 3) {
			// Initialize SHCT[evicted.signature_m] if you haven't already
			if (!SHCT.containsKey(got_evict.signature_m)) {
				SHCT.put(got_evict.signature_m, 0);
			}
			
			// if evicted.outcome == false
			if (!got_evict.get_outcome()) {
				// decrement SHCT[signature_m]
				SHCT.put(got_evict.signature_m, SHCT.get(got_evict.signature_m) - 1);
				if (SHCT.get(got_evict.signature_m) < 0)
					SHCT.put(got_evict.signature_m, 0);
			}
			Block_Cache newCache = UCL2_list.get(idx);
			
			newCache.set_outcome(false);
			
			// Initialize SHCT[newCache.signature_m] if you haven't already
			if (!SHCT.containsKey(newCache.signature_m)) {
				SHCT.put(newCache.signature_m, 0);
			}

			
			if (SHCT.get(newCache.signature_m) == 0) {
				// predict distant re-reference
				// For LRU this means make the counter 0 and increment all other LRU counters
				for (Block_Cache cb : UCL2_list) {
					cb.set_block_Cache_AccessCounter_LRU(cb.get_block_Cache_AccessCounter_LRU() + 1);
				}
				newCache.set_block_Cache_AccessCounter_LRU(0);
				UCL2_list.set(idx, newCache);
			}
			else {
				// predict intermediate re-reference
				// for LRU this means proceed as normal

			}
		}

		//Hawkeye
		else if (obj_cache.replacementPolicy == 4){
			//remove all instances (detrianing sampler)
			for (int i =0; i< blk.size();i++){
				if (blk.contains(UCL2_list.get(idx).block_cache_data)){
					blk.remove(i);
					counter.remove(i);
				}
			}
			//change block from cache
			Block_Cache newCache = UCL2_list.get(idx);
			UCL2_list.set(idx, newCache);
		}

		
	}

	void evict_L1(Block_Cache evicted) {
		
		int eL1_index = getting_idx_L1(SSMap.get(evicted.get_block_cache_data()));
		String eL1_tag = getting_tag_L1(SSMap.get(evicted.get_block_cache_data()));
		List<Block_Cache> li = obj_cache.newL1.get(eL1_index);
		for(Block_Cache cb: li) {
			if (cb.get_block_cache_Tag().equals(eL1_tag)) {
				int idx = li.indexOf(cb);
				Block_Cache eL1_var = li.get(idx);
				
				// if blank_Idx_L1 already contains idx do nothing
				if (!blank_Idx_L1.get(eL1_index).contains(idx)) {
					blank_Idx_L1.get(eL1_index).add(idx);
					blank_Flag_L1.set(eL1_index, blank_Flag_L1.get(eL1_index) + 1);
				}
				
				Collections.sort(blank_Idx_L1.get(eL1_index));
				li.set(idx, new Block_Cache("", "", 0, false));
				
				if (eL1_var.is_block_cache_dirtyBit())
					eviction_L1++;
				
				break;
			}
		}
	}
	
	String replacementPolicyToString(int policy) {
		switch (policy) {				
		case 0:
			return "LRU";
		case 1:
			return "Psudo-LRU";
		case 2:
			return "optimal";
		case 3:
			return "SHiP";
		case 4:
			return "Hawkeye";
				
		default:
			return "Invalid replacement Policy";
		}
	}
	
	void print_Cache() {
		System.out.println("===== Simulator configuration =====");
		System.out.println("BLOCKSIZE:             "	+	obj_cache.blockSize);
		System.out.println("L1_SIZE:               "	+	obj_cache.size_L1);
		System.out.println("L1_ASSOC:              "	+	obj_cache.Assoc_L1);
		System.out.println("L2_SIZE:               "	+	obj_cache.size_L2);
		System.out.println("L2_ASSOC:              "	+	obj_cache.Assoc_L2);
		System.out.println("REPLACEMENT POLICY:    "	+	REPLACEMENT_POLICIES[obj_cache.replacementPolicy]);
		System.out.println("INCLUSION PROPERTY:    "	+	(obj_cache.inclusionProperty == 0?"non-inclusive":"inclusive"));
		System.out.println("trace_file:            "	+	obj_cache.trace_File);
		
		// Print Contents of L1 Cache
		System.out.println("===== L1 contents =====");
		for (int i = 0; i < obj_cache.newL1.size(); i++) 
		{
			System.out.print("Set     " + String.format("%-8s", i + ":"));
			for(Block_Cache cb: obj_cache.newL1.get(i)) {
				System.out.print(binaryToHex(cb.get_block_cache_Tag()) + String.format("%-4s", cb.is_block_cache_dirtyBit()?" D":""));
			}
			System.out.println();
		}

		// Print Contents of L2 Cache (if available)
		if(obj_cache.newL2.size() != 0)
		{
			System.out.println("===== L2 contents =====");
			for (int i = 0; i < obj_cache.newL2.size(); i++) 
			{
				System.out.print("Set     " + String.format("%-8s", i+":"));
				for(Block_Cache cb: obj_cache.newL2.get(i)) {
					System.out.print(binaryToHex(cb.get_block_cache_Tag()) + String.format("%-5s", cb.is_block_cache_dirtyBit()?" D":""));
				}
				System.out.println();
			}
		}

		System.out.println("===== Simulation results (raw) =====");
		System.out.println("a. number of L1 reads:        "	+	reads_L1);
		System.out.println("b. number of L1 read misses:  "	+	read_Miss_L1);
		System.out.println("c. number of L1 writes:       "	+	writes_L1);
		System.out.println("d. number of L1 write misses: "	+	write_Miss_L1);

		// Here to fix formatting to match validation output
		// https://stackoverflow.com/questions/703396/how-to-nicely-format-floating-numbers-to-string-without-unnecessary-decimal-0s
		String l1_miss_rate = "";
		if (getting_MissRate_L1() % 1.0 != 0) 
		{
			l1_miss_rate = String.format("%.6f", getting_MissRate_L1());
		}
		else 
		{
			l1_miss_rate = String.format("%.0f", getting_MissRate_L1());
		}

		System.out.println("e. L1 miss rate:              "	+	l1_miss_rate);
		System.out.println("f. number of L1 writebacks:   "	+	write_backs_L1);
		System.out.println("g. number of L2 reads:        "	+	reads_L2);
		System.out.println("h. number of L2 read misses:  "	+	read_miss_L2);
		System.out.println("i. number of L2 writes:       "	+	writes_L2);
		System.out.println("j. number of L2 write misses: "	+	write_Miss_L2);

		String l2_miss_rate = "";
		if (getting_MissRate_L2() % 1.0 != 0) 
		{
			l2_miss_rate = String.format("%.6f", getting_MissRate_L2());
		}
		else 
		{
			l2_miss_rate = String.format("%.0f", getting_MissRate_L2());
		}

		System.out.println("k. L2 miss rate:              "	+	l2_miss_rate);
		System.out.println("l. number of L2 writebacks:   "	+	write_backs_L2);
		System.out.println("m. total memory traffic:      "	+ Memory_Collection);
	}

	double getting_MissRate_L1() {
		return (double)(read_Miss_L1 + write_Miss_L1)/(double)(reads_L1 + writes_L1);
	}

	double getting_MissRate_L2() {
		return (double)(read_miss_L2)/(double)(read_Miss_L1 + write_Miss_L1);
	}
	
	String binaryToHex(String str)
	{
		return new BigInteger(str,2).toString(16);
	}
}