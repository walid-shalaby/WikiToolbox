package wiki.toolbox.wiki;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.bzip2.CBZip2InputStream;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;

class SolrUpdateThread implements Runnable{
	String solr;
	String[] docs;
	int count;
	boolean flymode = false;
	boolean unknown = true;
	SolrUpdateThread(String solr, String[] docs, int count, boolean flymode, boolean unknown) {
		this.solr = solr;
		this.docs = docs;
		this.count = count;
		this.flymode = flymode;
		this.unknown = unknown;
	}
	
	@Override
	public void run() {
		String allxml = "";
		for(int i=0; i<count; i++) {
			String id, xml = "";
			int indx = docs[i].indexOf("\t");		
			if(indx>0) {
				// extract id
				id = docs[i].substring(0,docs[i].indexOf("\t"));
				if(SolrUpdate.ids.contains(id)==false) {
					//System.out.println(String.format("Adding (%s)...", id));
					System.out.println(docs[i]);
					
					// extract document xml
					xml = docs[i].substring(docs[i].indexOf("\t")+1);
					xml = xml.replace("\\n", "\n");
				}
			}
			else {
				if(unknown==false)
					continue;
				
				// extract id
				id = "unknown";
				System.out.println(String.format("Adding (%s)...", id));
				
				// extract document xml
				xml = docs[i];
				xml = xml.replace("\\n", "\n");
			}
			if(xml.length()>0) {
				if(xml.substring(0,5).compareTo("<add>")==0)
					xml = xml.substring(5);
				if(xml.substring(xml.length()-6,xml.length()).compareTo("</add>")==0)
					xml = xml.substring(0,xml.length()-6);
				allxml += xml;
			}
		}
		if(allxml.length()>0) {
			allxml = "<add>" + allxml + "</add>";
			SolrUpdate.sendUpdate(allxml, solr, flymode, this.toString());				
		}
	}
}

public class SolrUpdate {
	static public HashSet<String> ids = new HashSet<String>();
	protected static void printUsage() {
		System.out.println("Usage: java SolrUpdate --input input-dir --solr url-of-solr-update [--threads n] [--compressed y/n] [--thread-docs 100] [--flymode y/n] [--unknown y/n] [--ids path]");
		System.out.println("E.g.,: java SolrUpdate --input /home/wshalaby/solr-update --solr http://localhost:8983/solr/update --threads 10 --thread-docs 100 --flymode n --unknown y");
	}
	/*public static void main(String[] args) {
		HttpSolrServer server = new HttpSolrServer("http://localhost:8983/solr/wikipedia");
	    ModifiableSolrParams conceptsQParams = new ModifiableSolrParams();
	    conceptsQParams.set(CommonParams.Q, "title:*");
	    conceptsQParams.set(CommonParams.ROWS, Integer.MAX_VALUE);
	    
	    ArrayList<String> fl = new ArrayList<String>();
	    fl.add("title");
	        
	    // add target extract field
	    conceptsQParams.set("fl", fl.toArray(new String[fl.size()]));
	    QueryResponse conceptsQResp;
	    try {
	      conceptsQResp = server.query(conceptsQParams);
	    
	      // loop on results and add to concepts
	      SolrDocumentList results = conceptsQResp.getResults();
	      if(results!=null && results.size()>0) {
	    	  BufferedWriter bw = new BufferedWriter(new FileWriter("./ids.txt"));
	        String title = "http://localhost:8983/solr/wikipedia";
	        for(int i=0; i<results.size(); i++) {
	          SolrDocument doc = results.get(i);	          
	          // retrieve title
	          title = ((ArrayList<String>)doc.getFieldValue("title")).get(0);
	          bw.write(title+"\n");	          
	        }
	        bw.close();
	      }
	    } catch (SolrServerException | IOException e) {
	    	e.printStackTrace();      
	    }
	}*/
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/*
		 * For each file in passed arguments
		 * 1. open the file
		 * 2. read next line (each line corresponds to as id+tab+document in xml)
		 * 4. call solr update to add it to the index
		 */
		String inpath = "";
		String solr = "";
		String idspath = "";
		int numThreads = 1;
		int threads_docs = 100;
		boolean compressed = false;
		boolean flymode = false;
		boolean unknown = true;		
		ExecutorService executor;
		if(args.length>=4) {
			for(int i=0; i<args.length; i++) {
				if(args[i].equalsIgnoreCase("--input")) {
					inpath = args[i+1];
				}
				else if(args[i].equalsIgnoreCase("--solr")) {
					solr = args[i+1];
				}
				else if(args[i].equalsIgnoreCase("--threads")) {
					numThreads = Integer.parseInt(args[i+1]);
				}
				else if(args[i].equalsIgnoreCase("--compressed")) {
					compressed = args[i+1].equalsIgnoreCase("y");
				}
				else if(args[i].equalsIgnoreCase("--flymode")) {
					flymode = args[i+1].equalsIgnoreCase("y");
				}
				else if(args[i].equalsIgnoreCase("--unknown")) {
					unknown = args[i+1].equalsIgnoreCase("y");
				}
				else if(args[i].equalsIgnoreCase("--thread-docs")) {
					threads_docs = Integer.parseInt(args[i+1]);
				}
				else if(args[i].equalsIgnoreCase("--ids")) {
					idspath = args[i+1];
				}				
			}
			if(inpath.length()>0 && solr.length()>0) {
				if(idspath.length()>0)
					loadIDs(idspath);
				
				String[] docs = new String[threads_docs];
				executor = Executors.newFixedThreadPool(numThreads);
				File[] dir = new File(inpath).listFiles();
				for(File input : dir) {
					try {
						BufferedReader reader;
						if(compressed) {
							FileInputStream fis = new FileInputStream(input);
							fis.read();
							fis.read();
							CBZip2InputStream bz = new CBZip2InputStream(fis);
							reader = new BufferedReader(new InputStreamReader(bz));							
						}
						else {
							reader = new BufferedReader(new FileReader(input));
						}
						String line;
						int count = 0;
						while((line=reader.readLine())!=null) {
							if(numThreads>1){	
								docs[count] = line;
								count++;
								if(count==threads_docs) {
									executor.execute(new SolrUpdateThread(solr, docs, count, flymode, unknown));
									docs = new String[threads_docs];
									count = 0;
								}
							}
							else {
								addDoc(line, solr, flymode, unknown);
							}
						}
						if(count>0 && numThreads>1) {
							executor.execute(new SolrUpdateThread(solr, docs, count, flymode, unknown));
						}
						reader.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				//while(true);
				executor.shutdown();
				/*try {
					executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
				while(!executor.isTerminated()) {							
				}
				while(true);
			}
			else printUsage();
		}
		else 
			printUsage();
	}
	private static void addDoc(String docxml, String solr, boolean flymode, boolean unknown) {
		// TODO Auto-generated method stub		
		String id, xml = "";
		int indx = docxml.indexOf("\t");		
		if(indx>0) {
			// extract id
			id = docxml.substring(0,docxml.indexOf("\t"));
			if(SolrUpdate.ids.contains(id)==false) {
				System.out.print(String.format("Adding (%s)...", id));
				//System.out.println(docs[i]);
				
				// extract document xml
				xml = docxml.substring(docxml.indexOf("\t")+1);
				xml = xml.replace("\\n", "\n");
			}
		}
		else if(unknown==true) {
			// extract id
			id = "unknown";
			System.out.print(String.format("Adding (%s)...", id));
			
			// extract document xml
			xml = docxml;
			xml = xml.replace("\\n", "\n");
		}
		if(xml.length()>0) {
			sendUpdate(xml, solr, flymode, "1");
			
		}
		
	}
	public static void sendUpdate(String xml, String solr, boolean flymode, String threadid) {
		// TODO Auto-generated method stub
		try {
			String tempfilename = "";
			File tempfile = null;
			if(flymode==false) {
				// store to temporary file
				tempfilename = "./__temporary__patent__xml__"+threadid;
				tempfile = new File(tempfilename);
				FileWriter xmlfile;
				xmlfile = new FileWriter(tempfile);
				xmlfile.write(xml);
				xmlfile.close();
			}
	
			// invoke solr update on temporary file content
			ProcessBuilder cmd;
			if(System.getProperty("os.name").contains("Windows")) {
				if(flymode==true)
					cmd = new ProcessBuilder("curl", "-H","\"Content-Type: text/xml\"","\"Accept-Charset: UTF-8\"","\""+solr+"\"", "\"--data-binary\"","\""+xml+"\"");
				else
					cmd = new ProcessBuilder("curl", "-H","\"Content-Type: text/xml\"","\"Accept-Charset: UTF-8\"","\""+solr+"\"", "\"--data-binary\"","\"@"+tempfilename+"\"");
			}
			else {
				if(flymode==true)
					cmd = new ProcessBuilder("curl","-H","Content-Type: text/xml","Accept-Charset: UTF-8",solr, "--data-binary",xml);
				else
					cmd = new ProcessBuilder("curl","-H","Content-Type: text/xml","Accept-Charset: UTF-8",solr, "--data-binary","@"+tempfilename);
			}
			cmd.redirectErrorStream(true);
			Process p = cmd.start();
	
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String out, output="";
			while((out=br.readLine())!=null) {
				output += out + System.lineSeparator();
			}
			br.close();
			
			// print invocation result
			System.out.println("exit value: ("+String.valueOf(p.waitFor())+")");
			//System.out.println(output);
			

			// commit changes
			if(System.getProperty("os.name").contains("Windows"))
				cmd = new ProcessBuilder("curl", "\""+solr+"?commit=true\"");
			else
				cmd = new ProcessBuilder("curl", solr+"?commit=true");
			cmd.redirectErrorStream(true);
			p = cmd.start();
			
			// delete temporary file
			if(flymode==false)
				tempfile.delete();					
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private static void loadIDs(String idspath) {
		// TODO Auto-generated method stub
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(idspath));
			String line;
			while((line=br.readLine())!=null) {
				ids.add(line);
			}
			br.close();
			System.out.println("Loaded: ("+ids.size()+")");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

}