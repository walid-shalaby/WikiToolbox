package wiki.toolbox.wiki;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

import wiki.toolbox.semantic.SemanticSearchConfigParams;
import wiki.toolbox.semantic.SemanticsGenerator;

class SemanticsUtils {
	public static void doRelatednessExperiment(SemanticSearchConfigParams params) {
		// TODO Auto-generated method stub
		SemanticsGenerator semanticsGenerator = new SemanticsGenerator();
		semanticsGenerator.cacheAssociationsInfo("./wiki_associations.txt");
		semanticsGenerator.cacheConceptsInfo(params.wikiUrl, false, false);
		semanticsGenerator.doSemanticRelatedness("", "", params);
	}
	
	public static void doGenerateSemantics(SemanticSearchConfigParams params) {
		// TODO Auto-generated method stub
		try {
			// open source file
			BufferedReader in = new BufferedReader(new FileReader(params.in_path));
			if(in!=null)
			{
				ExecutorService executor = Executors.newFixedThreadPool(params.numThreads);
				
				String[][] samples = new String[params.blockSize][];
				String[] labels = new String[params.blockSize];
				HashSet<String> labelsDic = new HashSet<String>();
				
				// loop on input records
				int lineNo = 0;
				String line = in.readLine();
				while(line!=null)
				{
					// extract sample string
					// each sample is label\tid\tstring
					samples[lineNo] = line.split("\t");
					
					// extract label
					labels[lineNo] = samples[lineNo][0];
					
					// Create label directory if not there
					if(labelsDic.contains(labels[lineNo])==false) {
						File dir = new File(params.out_path+"/"+labels[lineNo]);
						if(dir.exists()==false)
							dir.mkdirs();
						labelsDic.add(labels[lineNo]);
					}
					if(++lineNo%params.blockSize==0) {
						executor.execute(new SemanticsGeneratorMain(samples, labels, params));
						samples =  new String[params.blockSize][];
						labels = new String[params.blockSize];
						lineNo = 0;
					}
					line = in.readLine();
				}
				if(lineNo>0) {
					executor.execute(new SemanticsGeneratorMain(samples, labels, params));
				}
				
				in.close();
				
				// wait for all threads to complete.
				executor.shutdown();
				while(!executor.isTerminated()) {
					
				}
			}
			System.out.println("Done generating semantics information :)");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
			e.printStackTrace();
		}
	}
}
class SemanticsCommandsMain implements Runnable{
	String command;
	
	SemanticsCommandsMain(String command) {
		this.command = command;
	}
	
	@Override
	public void run() {
		SemanticSearchConfigParams cfg = new SemanticSearchConfigParams();
		if(cfg.parseOpts(command.split(" "))==false)
			SemanticsGeneratorMain.displayUsage();
		else
		{
			if(cfg.hidden_relatedness_experiment==true) {
				SemanticsUtils.doRelatednessExperiment(cfg);
			}
			else {
				SemanticsUtils.doGenerateSemantics(cfg);
			}	
		}
	}
}

public class SemanticsGeneratorMain implements Runnable{

	SemanticsGenerator semanticsGenerator = null;
	SemanticSearchConfigParams cfg = null;
	String[][] samples;
	String[] labels;
	
	public SemanticsGeneratorMain(String[][] samples, String[] lables, SemanticSearchConfigParams cfg) {
		// TODO Auto-generated constructor stub
		this.samples = samples;
		this.labels = lables;
		this.cfg = cfg;
		
		semanticsGenerator = new SemanticsGenerator();
		semanticsGenerator.cacheAssociationsInfo("./wiki_associations.txt");
		semanticsGenerator.cacheConceptsInfo(cfg.wikiUrl, false, false);
		//semanticsGenerator.cachedConceptsInfo = new HashMap<String,CachedConceptInfo>();
	}
	
	@Override
	public void run() {
		// open output file
		for(int s=0; s<samples.length && samples[s]!=null; s++)
		{
			if(samples[s].length!=3)
				System.out.println("Can't process: "+samples[s][1]);
			else
			{
				String[] sample = samples[s];
				String label = labels[s];
				try {					
					File f = new File(cfg.out_path+"/"+label+"/"+sample[1]);
					if(f.exists()==true && f.length()>0)
						continue;
					FileWriter out = new FileWriter(f);
					if(f.exists()) {
						String semantics = "";
						if(cfg.debug==true)
							System.out.println("processing: "+sample[1]);
						NamedList<Object> semanticConceptsInfo = semanticsGenerator.doSemanticSearch(sample[2], cfg);
						if(semanticConceptsInfo!=null){
							if(cfg.row_based) {									
									for(int i=0; i<semanticConceptsInfo.size(); i++) {
										if(cfg.write_ids)
											semantics += sample[1] + cfg.semantics_separator;
										
										if(cfg.write_content)
											semantics += sample[2] + cfg.semantics_separator;
										
								        SimpleOrderedMap<Object> obj = (SimpleOrderedMap<Object>)semanticConceptsInfo.getVal(i);
								        semantics += semanticConceptsInfo.getName(i);
								        if(cfg.write_sem_ids)
								        	semantics += cfg.semantics_separator + obj.get("docno");
								        if(cfg.write_weights)
								        	semantics += cfg.semantics_separator + obj.get("weight");
								        
								        semantics += cfg.file_separator;
								        }
									out.write(semantics);
									}
							else {
								if(cfg.write_ids)
									semantics += sample[1]+cfg.file_separator;
								if(cfg.write_content)
									semantics += sample[2]+cfg.file_separator;
								for(int i=0; i<semanticConceptsInfo.size(); i++) {
									SimpleOrderedMap<Object> obj = (SimpleOrderedMap<Object>)semanticConceptsInfo.getVal(i);
							        semantics += semanticConceptsInfo.getName(i)+cfg.semantics_separator;
							        if(cfg.write_weights)
							        	semantics += obj.get("weight")+cfg.semantics_separator;
								}
								out.write(semantics);
							}										
						}
						out.close();
					}
					else
						System.out.println("Can't create output file for "+sample[1]);
				} catch (IOException e) {
					// TODO Auto-generated catch block				
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//Scanner sc = new Scanner(System.in);
		//System.out.print("enter wiki url: ");
		//String wikiurl = sc.nextLine();
		//System.out.print("enter associaitons path: ");
		//String assopath = sc.nextLine();
		SemanticsGenerator semanticsGenerator = new SemanticsGenerator();
		semanticsGenerator.cacheAssociationsInfo("./wiki_associations.txt");
		semanticsGenerator.cacheConceptsInfo(new SemanticSearchConfigParams().wikiUrl, false, false);
		//sc.close();
		
		if(args.length>0) {
			if(args.length==2) {
				System.out.println(args[0]);
				System.out.println(args[1]);
				processFile(args[0], Integer.parseInt(args[1]));
			}
			else {
				SemanticSearchConfigParams cfg = new SemanticSearchConfigParams();
				if(cfg.parseOpts(args)==false)
					displayUsage();
				else
				{
					if(cfg.hidden_relatedness_experiment==true) {
						SemanticsUtils.doRelatednessExperiment(cfg);
					}
					else {
						SemanticsUtils.doGenerateSemantics(cfg);
					}	
				}
			}
		}
		else 
			doInteractiveMode();
	}
	private static void doInteractiveMode() {
		// TODO Auto-generated method stub
		while(true) {
			Scanner sc = new Scanner(System.in);			
			System.out.print("enter commands path: ");
			while(!sc.hasNextLine()) {
				sc.next();
			}
			String s = sc.nextLine();
			System.out.print("enter number of threads: ");
			int numThreads = sc.nextInt();
			sc.close();
			if(s.length()>0) {
				processFile(s, numThreads);
			}
			else
				break;
		}
	}

	private static void processFile(String filepath, int numThreads) {
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		try {
			// open source file
			BufferedReader in = new BufferedReader(new FileReader(filepath));
			if(in!=null) {
				// loop on input records
				String line = in.readLine();
				while(line!=null) {
					executor.execute(new SemanticsCommandsMain(line));
					line = in.readLine();
				}
				
				in.close();
				
				// wait for all threads to complete.
				executor.shutdown();
				while(!executor.isTerminated()) {							
				}
			}
			System.out.println("Done processing commands :)");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
			e.printStackTrace();
		}
	}
	static void displayUsage() {
		System.out.println("java -jar semantics_generator.jar "
				+ "--url path-to-wiki-solr-instance "
				+ "--input path-to-input-file "
				+ "--output path-to-output "
				+ "--method Explicit|MSA_anchors|MSA_seealso|MSA_anchors_seealso|MSA_seealso_asso|MSA_anchors_seealso_asso "
				+ "[--max-hits number-of-initial-hits] "
				+ "[--concepts-num number-of-semantic-concepts] "
				+ "[--max-title-ngrams maximum-ngrams-in-title] "
				+ "[--max-seealso-ngrams maximum-ngrams-in-seealso-title] "
				+ "[--field target-search-field] "
				+ "[--min-len minimum-doc-length] "
				+ "[--min-seealso-len minimum-seealso-doc-length] "
				+ "[--min-asso-cnt minimum-accociation-count] "
				+ "[--semantics-separator tab|newline|string] "
				+ "[--file-separator tab|newline|string] "
				+ "[--threads-num num-threads-to-run ]"
				+ "[--block-size samples-per-thread ]"
				+ "[--extra-q extra-query [on/off]] "
				+ "[--relatedness-expr [on/off]] "
				+ "[--distance cosine|bin|cosinenorm|wo|euclidean] "
				+ "[--title-search [on/off]] "
				+ "[--relax-same-title [on/off]] "
				+ "[--abs-explicit [on/off]] "
				+ "[--relax-cache [on/off]] "
				+ "[--relax-seealso [on/off]] "
				+ "[--relax-listof [on/off]] "
				+ "[--relax-ner [on/off]] "
				+ "[--relax-disambig [on/off]] "
				+ "[--relax-categories [on/off]] "
				+ "[--wikiurl wiki-index-url] "
				+ "[--write-ids [on/off]] ");
	}
}
