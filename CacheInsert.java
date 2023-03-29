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
		insert_Cache_Data();
	}
	//Get index from L1 of the address
	int getting_idx_L1(String str_idx_L1)
	{
		return Integer.parseInt(str_idx_L1.substring(obj_cache.tag_L1, obj_cache.tag_L1+ obj_cache.idx_L1),2);
	}
	//Get index from L2 of the address
	int getting_idx_L2(String str_idx_L2)
	{
		return Integer.parseInt(str_idx_L2.substring(obj_cache.tag_L2, obj_cache.tag_L2+ obj_cache.idx_L2),2);
	}
	String getting_tag_L1(String str_tag_L1)
	{
		return str_tag_L1.substring(0, obj_cache.tag_L1);
	}
	String getting_tag_L2(String str_tag_L2)
	{
		return str_tag_L2.substring(0, obj_cache.tag_L2);
	}
	class Node{
		String node_str;
		int node_index;
		public Node(String str, int index) {
			super();
			this.node_str = str;
			this.node_index = index;
		}
		public String getNode_str() {
			return node_str;
		}
		public int getNode_index() {
			return node_index;
		}
	}
	Map<Integer, List<Node>> hex = new HashMap<>();
	void insert_Cache_Data() {
		int i1=0;
		while(i1<SData.size()){
			String ICD_str = SData.get(i1);
			String ICD_temp = ICD_str.split(" ")[1];
			int ICD_index = getting_idx_L1(SSMap.get(ICD_temp));
			if(!hex.containsKey(ICD_index))
				hex.put(ICD_index, new ArrayList<>());
			hex.get(ICD_index).add(new Node(ICD_temp,i1));
			i1++;
		}
		int i2=0;
		while(i2<SData.size()){
			String ICD_str = SData.get(i2);
			global_Idx_Optimal = i2;
			boolean ICD_write_read = ICD_str.split(" ")[0].equals("r");
			ICD_str = ICD_str.split(" ")[1];
			if(ICD_write_read)
				reading_L1(ICD_str, SSMap.get(ICD_str));
			else
				writing_L1(ICD_str, SSMap.get(ICD_str));
			i2++;
		}
		if(obj_cache.newL2.size() == 0)
		{
			Memory_Collection = read_Miss_L1 + write_Miss_L1 +write_backs_L1;
		}
		else
			Memory_Collection = read_miss_L2 + write_Miss_L2 +write_backs_L2 + eviction_L1;
		print_Cache();
	}
	//psudoLRU
	void alloc(int arr[], int mid, int index, int lvl_val, int v)
	{
		if(lvl_val == 0)
		{
			arr[index] = v;
			return;
		}
		else if(mid>index)
		{
			arr[mid] = 0;
			alloc(arr, mid - lvl_val, index, lvl_val/2, v);
		}
		else
		{
			arr[mid] = 1;
			alloc(arr, mid + lvl_val, index, lvl_val/2, v);
		}
	}
	void plru_update(int[] arr, int index) {
		int idx = index;
		int v = 0;
		if(index%2 != 0)
		{
			idx --;
			v = 1;
		}
		int mid = (arr.length-1)/2;
		alloc(arr, mid, idx, (mid+1)/2, v);
	}
	int blank_Flag_L1 = 0;
	List<Integer> blank_Idx_L1 = new ArrayList<>();
	int row_Idx = 0;
	//reading L1 cache
	void reading_L1(String rL1_data, String rL1_bits) {
		List<Block_Cache> rL1_list = obj_cache.newL1.get(getting_idx_L1(rL1_bits));
		String rL1_tag = getting_tag_L1(rL1_bits);
		reads_L1++;
		for(Block_Cache bc: rL1_list)
		{
			if(bc.block_cache_tag.equals(rL1_tag)) {
				hit_Read_L1(rL1_tag, rL1_list, bc);
				//psudolru
				plru_update(obj_cache.newplru_L1[getting_idx_L1(rL1_bits)], rL1_list.indexOf(bc));
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
			if(blank_Flag_L1 != 0)
			{
				rL1_list.add(blank_Idx_L1.get(0),new Block_Cache(rL1_data, rL1_tag, obj_cache.set_L1 -1 , false));
				plru_update(obj_cache.newplru_L1[getting_idx_L1(rL1_bits)], blank_Idx_L1.remove(0));
				blank_Flag_L1--;
			}
			else
			{
				rL1_list.add(new Block_Cache(rL1_data, rL1_tag, obj_cache.set_L1 -1 , false));
				plru_update(obj_cache.newplru_L1[getting_idx_L1(rL1_bits)], rL1_list.size()-1);
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
	//hit in read in L1
	void hit_Read_L1(String hRL1_tag, List<Block_Cache> hRL1_list, Block_Cache hRL1_c) {
		//lru
		int hRL1_val = hRL1_c.get_block_Cache_AccessCounter_LRU();
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
				//psudolru
				plru_update(obj_cache.newplru_L1[getting_idx_L1(wL1_bits)], wL1_list.indexOf(bc));
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
			if(blank_Flag_L1 != 0)
			{
				wL1_list.add(blank_Idx_L1.get(0),new Block_Cache(wL1_data, wL1_tag, obj_cache.set_L1 -1 , true));
				plru_update(obj_cache.newplru_L1[getting_idx_L1(wL1_bits)], blank_Idx_L1.remove(0));
				blank_Flag_L1--;
			}
			else
			{
				wL1_list.add(new Block_Cache(wL1_data, wL1_tag, obj_cache.set_L1 -1 , true));
				plru_update(obj_cache.newplru_L1[getting_idx_L1(wL1_bits)], wL1_list.size()-1);
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
				//psudolru
				plru_update(obj_cache.newplru_L2[getting_idx_L2(rL1_bits)], rL1_list.indexOf(bc));
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
			rL1_list.add(new Block_Cache(rL1_data, rL1_tag, obj_cache.set_L2 -1 , false));

			//psudolru
			plru_update(obj_cache.newplru_L2[getting_idx_L2(rL1_bits)], rL1_list.size()-1);
		}
		else //using replacement policy
		{
			u_Cache_L2(rL1_data, rL1_tag, rL1_list, true);
		}
	}
	//hit read in L2
	void hit_Read_L2(String hRL2_tag, List<Block_Cache> hRL2_list, Block_Cache hRL2_c) {
		int hRL2_val = hRL2_c.get_block_Cache_AccessCounter_LRU();

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
				//psudo lru
				plru_update(obj_cache.newplru_L2[getting_idx_L2(wL2_bits)], wL2_list.indexOf(bc));
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
			wL2_list.add(new Block_Cache(wL2_data, wL2_tag, obj_cache.set_L2 -1 , true));

			//psudo lru
			plru_update(obj_cache.newplru_L2[getting_idx_L2(wL2_bits)], wL2_list.size()-1);
		}
		else //applying replacement policy
		{
			u_Cache_L2(wL2_data, wL2_tag, wL2_list, false);
		}
	}
	//hit write in L2
	void hit_Write_L2(String hWL2_tag, List<Block_Cache> hWL2_list, Block_Cache hWL2_c) {
		int hWL2_val = hWL2_c.get_block_Cache_AccessCounter_LRU();

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
	int getting_eviction_idx_plru(int[] arr) {
		int new_mid = (arr.length-1)/2;
		int lvl_val = (new_mid+1)/2;
		return remove_allocation(new_mid,lvl_val,arr);
	}
	int remove_allocation(int mid, int lvl_val, int[] arr) {
		if(lvl_val == 0)
		{
			if(arr[mid] == 0)
			{
				arr[mid] = 1;
				return mid+1;
			}
			else
			{
				arr[mid] = 0;
				return mid;
			}
		}
		else if(arr[mid] == 0)
		{
			arr[mid] = 1;
			return remove_allocation(mid + lvl_val, lvl_val/2, arr);
		}
		else
		{
			arr[mid] = 0;
			return remove_allocation(mid - lvl_val, lvl_val/2, arr);
		}
	}
	//L1 replacement policy
	void updating_Cache_L1(String u_data, String u_tag, List<Block_Cache> u_list, boolean u_read) {
		int uCL1_idx = 0;
		switch(obj_cache.replacementPolicy)
		{
			case 1:{
				uCL1_idx = getting_eviction_idx_plru(obj_cache.newplru_L1[getting_idx_L1(SSMap.get(u_data))]);
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
			default:{

				int i3=0;
				while(i3<u_list.size()){
					Block_Cache cb = u_list.get(i3);
					if(cb.get_block_Cache_AccessCounter_LRU() == 0)
					{
						uCL1_idx = i3;
					}
					else
					{
						cb.set_block_Cache_AccessCounter_LRU(cb.get_block_Cache_AccessCounter_LRU()-1);
					}
					i3++;
				}
				break;
			}
		}
		Block_Cache evicted = u_list.remove(uCL1_idx);
		if(evicted.is_block_cache_dirtyBit())
		{
			write_backs_L1++;
		}
		u_list.add(uCL1_idx, new Block_Cache(u_data, u_tag, obj_cache.set_L1 -1 , true));
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
		int i4=0;
		while (i4 < arr.length){
			max = Math.max(i4, max);
			i4++;
		}
		int leastUsed = -1;
		int i5=0;
		while(i5<gvopt_li.size()){
			if(arr[i5] == max && leastUsed < gvopt_li.get(i5).block_Cache_AccessCounter_OPT())
			{
				leastUsed = gvopt_li.get(i5).block_Cache_AccessCounter_OPT();
				evopt_idx = i5;
				return evopt_idx;
			}
			i5++;
		}
		return evopt_idx;
	}
	//L2 replacement policy
	void u_Cache_L2(String UCL2_data, String UCL2_tag, List<Block_Cache> UCL2_list, boolean UCL2_read) {
		int idx = 0;
		if (obj_cache.replacementPolicy == 1)
		{
			idx = getting_eviction_idx_plru(obj_cache.newplru_L2[getting_idx_L2(SSMap.get(UCL2_data))]);
		}
		else if (obj_cache.replacementPolicy == 2) {
			idx = getting_eviction_idx_opt(UCL2_list);
			int val = UCL2_list.get(idx).block_Cache_AccessCounter_OPT();
			for (Block_Cache cb : UCL2_list) {
				if (cb.block_Cache_AccessCounter_OPT() < val)
					cb.set_block_Cache_AccessCounter_OPT(cb.block_Cache_AccessCounter_OPT() + 1);
			}
		}
		else
		{
			int i6=0;
			while(i6<UCL2_list.size()){
				Block_Cache cb = UCL2_list.get(i6);
				if(cb.get_block_Cache_AccessCounter_LRU() == 0)
				{
					idx = i6;
				}
				else
				{
					cb.set_block_Cache_AccessCounter_LRU(cb.get_block_Cache_AccessCounter_LRU()-1);
				}
				i6++;
			}
		}
		Block_Cache got_evict = UCL2_list.remove(idx);
		if(got_evict.is_block_cache_dirtyBit())
		{
			write_backs_L2++;
		}
		UCL2_list.add(idx, new Block_Cache(UCL2_data, UCL2_tag, obj_cache.set_L2 -1 , true));
		if(UCL2_read)
		{
			UCL2_list.get(idx).set_block_cache_dirtyBit(false);
		}
		if(obj_cache.inclusionProperty == 1)
		{
			evict_L1(got_evict);
		}
	}
	void evict_L1(Block_Cache evicted) {
		int eL1_index = getting_idx_L1(SSMap.get(evicted.get_block_cache_data()));
		String eL1_tag = getting_tag_L1(SSMap.get(evicted.get_block_cache_data()));
		List<Block_Cache> li = obj_cache.newL1.get(eL1_index);
		for(Block_Cache cb: li)
		{
			if(cb.get_block_cache_Tag().equals(eL1_tag))
			{
				int idx = li.indexOf(cb);
				blank_Flag_L1++;
				blank_Idx_L1.add(idx);
				Block_Cache eL1_var = li.remove(idx);
				if(eL1_var.is_block_cache_dirtyBit())
					eviction_L1++;
				break;
			}
		}
	}
	void print_Cache() {
		System.out.println("============= Simulator configuration ============");
		System.out.println("BLOCKSIZE:             "	+	obj_cache.blockSize);
		System.out.println("L1_SIZE:               "	+	obj_cache.size_L1);
		System.out.println("L1_ASSOC:              "	+	obj_cache.Assoc_L1);
		System.out.println("L2_SIZE:               "	+	obj_cache.size_L2);
		System.out.println("L2_ASSOC:              "	+	obj_cache.Assoc_L2);
		System.out.println("REPLACEMENT POLICY:    "	+	(obj_cache.replacementPolicy == 0?"LRU":(obj_cache.replacementPolicy == 1?"Pseudo-LRU":"Optimal")));
		System.out.println("INCLUSION PROPERTY:    "	+	(obj_cache.inclusionProperty == 0?"non-inclusive":"inclusive"));
		System.out.println("trace_file:            "	+	obj_cache.trace_File);
		System.out.println("================== L1 contents ===================");
		int i7 = 0;
		while(i7< obj_cache.newL1.size()){
			System.out.print("Set     "+i7+":");
			for(Block_Cache cb: obj_cache.newL1.get(i7)) {
				System.out.print(" "+binaryToHex(cb.get_block_cache_Tag())+(cb.is_block_cache_dirtyBit()?" D":" "));
			}
			System.out.println();
			i7++;
		}
		if(obj_cache.newL2.size() != 0)
		{
			System.out.println("============== L2 contents ===============");
			int i8 = 0;
			while(i8< obj_cache.newL2.size()){
				System.out.print("Set     "+i8+":");
				for(Block_Cache cb: obj_cache.newL2.get(i8)) {
					System.out.print(" "+binaryToHex(cb.get_block_cache_Tag())+(cb.is_block_cache_dirtyBit()?" D":" "));
				}
				System.out.println();
				i8++;
			}
		}
		System.out.println("=============== Simulation results (raw) ==============");
		System.out.println("a. number of L1 reads:        "	+	reads_L1);
		System.out.println("b. number of L1 read misses:  "	+	read_Miss_L1);
		System.out.println("c. number of L1 writes:       "	+	writes_L1);
		System.out.println("d. number of L1 write misses: "	+	write_Miss_L1);
		System.out.println("e. L1 miss rate:              "	+	String.format("%.6f", getting_MissRate_L1()));
		System.out.println("f. number of L1 writebacks:   "	+	write_backs_L1);
		System.out.println("g. number of L2 reads:        "	+	reads_L2);
		System.out.println("h. number of L2 read misses:  "	+	read_miss_L2);
		System.out.println("i. number of L2 writes:       "	+	writes_L2);
		System.out.println("j. number of L2 write misses: "	+	write_Miss_L2);
		System.out.println("k. L2 miss rate:              "	+	String.format("%.6f", getting_MissRate_L2()));
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
class Convert {
	static Map<Character,String> conv_Map = new HashMap<>() {{
		put('0',"0000");
		put('1',"0001");
		put('2',"0010");
		put('3',"0011");
		put('4',"0100");
		put('5',"0101");
		put('6',"0110");
		put('7',"0111");
		put('8',"1000");
		put('9',"1001");
		put('A',"1010");
		put('B',"1011");
		put('C',"1100");
		put('D',"1101");
		put('E',"1110");
		put('F',"1111");
	}};
	static String conv_hex_2_bin(String s)
	{
		String ret = "";
		s = s.toUpperCase();
		int x =0;
		while(x<s.length()){
			ret = ret + conv_Map.get(s.charAt(x));
			x++;
		}
		return ret;
	}
}
class Main_Caching_System {
	int MCS_Size_Block, MCS_Size_L1, MCS_Assoc_L1, MCS_Size_L2, MCS_Assoc_L2, MCS_Replacement_Policy, MCS_Inclusion_Property, MCS_Rows_L1, MCS_Rows_L2;
	String traceFile;
	//list and map declaration
	final List<String> new_obj_data;
	final Map<String,String> new_obj_map;
	final Cache new_obj_cache;
	//get command line parameter input
	public Main_Caching_System(int cons_MCS_Size_Block, int cons_MCS_Size_L1, int cons_MCS_Assoc_L1, int cons_MCS_Size_L2, int cons_MCS_Assoc_L2, int cons_MCS_Replacement_Policy,
							   int cons_MCS_Inclusion_Property, String cons_traceFile) throws IOException {
		this.MCS_Size_Block = cons_MCS_Size_Block;
		this.MCS_Size_L1 = cons_MCS_Size_L1;
		this.MCS_Assoc_L1 = cons_MCS_Assoc_L1;
		this.MCS_Size_L2 = cons_MCS_Size_L2;
		this.MCS_Assoc_L2 = cons_MCS_Assoc_L2;
		this.MCS_Replacement_Policy = cons_MCS_Replacement_Policy;
		this.MCS_Inclusion_Property = cons_MCS_Inclusion_Property;
		this.traceFile = cons_traceFile;
		// evaluating l1's and l2's rows and sets and cache architecture structuring
		func_MCS_Rows_Calc();
		new_obj_cache = new Cache(MCS_Rows_L1, cons_MCS_Assoc_L1, MCS_Rows_L2, cons_MCS_Assoc_L2, cons_MCS_Size_Block, cons_MCS_Replacement_Policy, cons_MCS_Inclusion_Property, cons_MCS_Size_L1, cons_MCS_Size_L2, cons_MCS_Assoc_L1, cons_MCS_Assoc_L2, cons_traceFile);
		// getting arraylist and hashmap data
		new_obj_data = new ArrayList<>();
		new_obj_map = new HashMap<>();
		//simplifying the data
		func_MCS_readF();
		//insert in cache
		func_MCS_insert();
	}
	void func_MCS_insert() {
		new CacheInsert(new_obj_cache, new_obj_map, new_obj_data);
	}
	void func_MCS_Rows_Calc() {
		//evaluating if row number is +ve or -ve
		MCS_Assoc_L1 = MCS_Assoc_L1 == 0? 1 : MCS_Assoc_L1;
		MCS_Assoc_L2 = MCS_Assoc_L2 == 0? 1 : MCS_Assoc_L2;
		//evaluating l1 and l2 rows
		MCS_Rows_L1 = MCS_Size_L1 / (MCS_Assoc_L1 * MCS_Size_Block);
		MCS_Rows_L2 = MCS_Size_L2 / (MCS_Assoc_L2 * MCS_Size_Block);
	}
	void func_MCS_readF() throws IOException {
		//getting and understanding trace file and applying exception if file not found
		File importF= new File("traces/" + traceFile);
		BufferedReader new_br = new BufferedReader(new FileReader(importF));
		String new_str;
		//check whether line containing instruction is valid or not?
		while((new_str = new_br.readLine()) != null)
		{
			//skip if not getting instruction
			if(new_str.length() == 0)
				continue;
			//instruction append
			new_obj_data.add(new_str);
			func_MCS_parse_Trace_Data(new_str);
		}
		//exit stream
		new_br.close();
	}
	// scanning the tracefile and get the data
	void func_MCS_parse_Trace_Data(String new_str) {
		new_str = new_str.trim();
		String pointer;
		//splitting the instruction line into action and hexcode
		pointer = new_str = new_str.split(" ")[1];
		if(new_obj_map.containsKey(pointer))
			return;
		//concatenate 0 if instruction hexcode is not 8 bit
		String concatenate_zeros = "00000000";
		if(pointer.length() != 8)
		{
			pointer = concatenate_zeros.substring(0 , 8 - new_str.length()) + pointer;
		}
		new_obj_map.put(new_str, Convert.conv_hex_2_bin(pointer));
	}
}
class Block_Cache {
	boolean block_cache_dirtyBit;
	String block_cache_data, block_cache_tag;
	int block_Cache_AccessCounter_LRU, block_Cache_AccessCounter_OPT;
	public Block_Cache(String cons_block_cache_data, String cons_block_cache_tag, int cons_block_Cache_AccessCounter_LRU, boolean cons_block_cache_dirtyBit) {
		super();
		this.block_cache_data = cons_block_cache_data;
		this.block_cache_tag = cons_block_cache_tag;
		block_Cache_AccessCounter_LRU = cons_block_Cache_AccessCounter_LRU;
		this.block_cache_dirtyBit = cons_block_cache_dirtyBit;
		block_Cache_AccessCounter_OPT = 0;
	}
	public String get_block_cache_data() {
		return block_cache_data;
	}
	public String get_block_cache_Tag() {
		return block_cache_tag;
	}
	public int get_block_Cache_AccessCounter_LRU() {
		return block_Cache_AccessCounter_LRU;
	}
	public void set_block_Cache_AccessCounter_LRU(int arg_lRUaccessCounter) {
		block_Cache_AccessCounter_LRU = arg_lRUaccessCounter;
	}
	public boolean is_block_cache_dirtyBit() {
		return block_cache_dirtyBit;
	}
	public void set_block_cache_dirtyBit(boolean arg_dirtyBit) {
		this.block_cache_dirtyBit = arg_dirtyBit;
	}
	public int block_Cache_AccessCounter_OPT() {
		return block_Cache_AccessCounter_OPT;
	}
	public void set_block_Cache_AccessCounter_OPT(int arg_AccessCounter_Optimal) {
		block_Cache_AccessCounter_OPT = arg_AccessCounter_Optimal;
	}
}
class Cache {
	List<List<Block_Cache>> newL1, newL2;
	int tag_L1, idx_L1, offset, set_L1, tag_L2, idx_L2, set_L2,
			replacementPolicy,inclusionProperty, blockSize, size_L1, Assoc_L1,
			size_L2, Assoc_L2;
	String trace_File;
	int logOfTwo(int y) {
		int a = (int) ((Math.log(y)) / (Math.log(2)));
		return a;
	}
	//structuring the Cache Memory
	Cache(int cons_idx_L1, int cons_set_L1, int cons_idx_L2, int cons_set_L2, int cons_blockSize, int cons_replacementPolicy, int cons_inclusionProperty, int cons_size_L1, int cons_size_L2, int cons_Assoc_L1, int cons_Assoc_L2, String cons_traceFile)
	{
		this.inclusionProperty = cons_inclusionProperty;
		this.replacementPolicy = cons_replacementPolicy;
		this.blockSize = cons_blockSize;
		this.set_L1 = cons_set_L1;
		this.set_L2 = cons_set_L2;
		newL1 = new ArrayList<>();
		newL2 = new ArrayList<>();
		List<Block_Cache> newtemp;
		int i9=0;
		while(i9<cons_idx_L1){
			newtemp = new ArrayList<>();
			newL1.add(newtemp);
			i9++;
		}
		int i10 = 0;
		while(i10<cons_idx_L2){
			newtemp = new ArrayList<>();
			newL2.add(newtemp);
			i10++;
		}
		offset = logOfTwo(blockSize);
		this.idx_L1 = logOfTwo(cons_idx_L1);
		this.idx_L2 = logOfTwo(cons_idx_L2);
		tag_L1 = 32 - (this.idx_L1 + offset);
		tag_L2 = 32 - (this.idx_L2 + offset);
		this.size_L1 = cons_size_L1;
		this.size_L2 = cons_size_L2;
		this.Assoc_L1 = cons_Assoc_L1;
		this.Assoc_L2 = cons_Assoc_L2;
		this.trace_File = cons_traceFile;
		init_plru();
	}
	int[][] newplru_L1;
	int[][] newplru_L2;
	int mid_plru_L1, mid_plru_L2;
	void init_plru() {
		int temp_set_L1 = set_L1;
		int temp_set_L2 = set_L2;
		if(set_L1 < 2)
			temp_set_L1 = 2;
		if(set_L2 <2)
			temp_set_L2 = 2;
		newplru_L1 = new int [newL1.size()][temp_set_L1-1];
		newplru_L2 = new int [newL2.size()][temp_set_L2-1];
		mid_plru_L1 = (temp_set_L1-2)/2;
		mid_plru_L2 = (temp_set_L2-2)/2;
	}
}