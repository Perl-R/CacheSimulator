import java.util.*;
import java.math.*;
import java.io.*;

class Hex_Converter {
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