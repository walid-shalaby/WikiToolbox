package wiki.toolbox.wiki;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RawIndexDataUpdater {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// retrieve all concepts
		System.out.println("Usage: jar rawindexupdater.jar path-to-sourcefile path-to-destinationfile path-to-outputfile");
		System.out.println("e.g., jar rawindexupdater.jar /path/to/update/file/source-then-tab-then-value /scratch/wshalaby/wikipedia/solrupdate/enwiki-20150304.txt /scratch/wshalaby/wikipedia/solrupdate/enwiki-20150304-with-pageranks.txt");
		update(args);
	}
	
	private static void update(String[] args) {
		// TODO Auto-generated method stub
		String sourcepath = args[0];
		String destpath = args[1];
		String outpath = args[2];
		
		HashMap<String,String> cache = new HashMap<String,String>();
		
		Pattern titlePat = Pattern.compile("<field name=\"title\">.*?</field>");
		
    	BufferedReader inrecords;
    	BufferedWriter outrecords;
		try {
			inrecords = new BufferedReader(new FileReader(sourcepath));
		
			String line = inrecords.readLine();
		    while(line!=null && line.length()>0) {
		    	String[] input = line.split("\t");
		    	cache.put(input[0], input[1]);
		    	line = inrecords.readLine();
		    }
		    inrecords.close();
		    
			inrecords = new BufferedReader(new FileReader(destpath));
			outrecords = new BufferedWriter(new FileWriter(outpath));
			
			line = inrecords.readLine();
		    while(line!=null && line.length()>0) {
		    	Matcher m = titlePat.matcher(line);
		    	  if(m.find()) {
		    		  String title = m.group().replace("<field name=\"title\"><![CDATA[", "").replace("]]></field>", "");
		    		  if(cache.containsKey(title)) {
		    			  line = line.replace("</doc></add>", "<field name=\"pagerank\"><![CDATA["+cache.get(title)+"]]></field></doc></add>");
		    			  outrecords.write(line+System.lineSeparator());
		    		  }
		    		  else
		    			  System.out.println(title+"...not found");
		    			  
		    	  }
		    	line = inrecords.readLine();
		    }
		    inrecords.close();
		    outrecords.close();
		}catch (IOException e) {
			e.printStackTrace();
		}			  
	}
}
