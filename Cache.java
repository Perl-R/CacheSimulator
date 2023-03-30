import java.util.*;
import java.math.*;
import java.io.*;

public class Cache {
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

		int i=0;
		while(i<cons_idx_L1){
			newtemp = new ArrayList<>();
			newL1.add(newtemp);
			i++;
		}
		
		i = 0;
		while(i<cons_idx_L2){
			newtemp = new ArrayList<>();
			newL2.add(newtemp);
			i++;
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
	}
}
