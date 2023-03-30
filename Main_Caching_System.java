import java.util.*;
import java.math.*;
import java.io.*;

public class Main_Caching_System {

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
		new_obj_map.put(new_str, Hex_Converter.conv_hex_2_bin(pointer));
	}
}
