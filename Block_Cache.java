import java.util.*;
import java.math.*;
import java.io.*;

class Block_Cache {
	boolean block_cache_dirtyBit;
	String block_cache_data, block_cache_tag;
	int block_Cache_AccessCounter_LRU, block_Cache_AccessCounter_OPT;
	// for SHiP
	boolean outcome;
	String signature_m;

	public Block_Cache(String cons_block_cache_data, String cons_block_cache_tag, int cons_block_Cache_AccessCounter_LRU, boolean cons_block_cache_dirtyBit) {
		super();
		this.block_cache_data = cons_block_cache_data;
		this.block_cache_tag = cons_block_cache_tag;
		block_Cache_AccessCounter_LRU = cons_block_Cache_AccessCounter_LRU;
		this.block_cache_dirtyBit = cons_block_cache_dirtyBit;
		block_Cache_AccessCounter_OPT = 0;
		// SHiP
		outcome = false;
		signature_m = cons_block_cache_tag + cons_block_cache_data;
	}
	public void set_outcome(boolean truthValue) {
		this.outcome = truthValue;
	}
	public boolean get_outcome() {
		return outcome;
	}
	public void set_signature_m(String i) {
		this.signature_m = i;
	}
	public String get_signature_m() {
		return signature_m;
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