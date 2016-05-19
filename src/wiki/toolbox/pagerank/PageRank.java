//---------------------------------------------------
//By: Walid Shalaby - 800844545 (wshalaby@uncc.edu) |
//---------------------------------------------------

//package org.myorg;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.fs.FileSystem;

public class PageRank extends Configured implements Tool {

   private static final Logger LOG = Logger .getLogger( PageRank.class);

   public static final double d = 0.85;
   
   public static void main( String[] args) throws  Exception {
      int res  = ToolRunner .run( new PageRank(), args);
      System .exit(res);
   }

   public int run( String[] args) throws  Exception {
	   int max_iterations = Integer.parseInt(args[2]);
	   
	  // link graph job
      Job job  = Job .getInstance(getConf(), " LinkGraph ");
      job.setJarByClass( this .getClass());

      FileInputFormat.addInputPaths(job,  args[0]);
      FileOutputFormat.setOutputPath(job,  new Path(args[1]+".tmp/linkgraph"));
      job.setMapperClass( LinkGraphMap.class);
      job.setReducerClass( LinkGraphReduce.class);
      job.setOutputKeyClass( Text.class);
      job.setOutputValueClass( Text.class);

      if(job.waitForCompletion(true)==true) {
    	  LOG.info("LinkGraph finished successfully");
    	  String inputpath = args[1]+".tmp/linkgraph";
    	  
    	  // start pagerank computations
    	  int iter = 1;
    	  for(; iter<=max_iterations; iter++) {
    		  job  = Job .getInstance(getConf(), " PageRank ");
              job.setJarByClass( this .getClass());
              job.setMapperClass( PageRankMap.class);
              job.setReducerClass( PageRankReduce.class);
              job.setOutputKeyClass( Text.class);
              job.setOutputValueClass( Text.class);
              FileInputFormat.addInputPaths(job,  inputpath);
        	  
              String outpath = args[1] + String.format(".tmp/pagerank%d",iter);
	          FileOutputFormat.setOutputPath(job,  new Path(outpath));
	          if(job.waitForCompletion(true)==true) {
	        	  LOG.info(String.format("PageRank finished iteration: (%d)",iter));
	        	  
	        	  // remove old directory unless it is linkgraph
	        	  if(iter!=1)
	        		  FileSystem.get(getConf()).delete(new Path(inputpath), true);
	        	  inputpath = outpath;
	          }
	          else
	        	  break;
    	  }
          if(iter>max_iterations) {
        	  FileSystem.get(getConf()).rename(new Path(inputpath), new Path(args[1]+".tmp/pagerank"));
        	  LOG.info("PageRank finished successfully");
        	  
        	  // sort by page rank
        	  job  = Job .getInstance(getConf(), " PageRankSorter ");
        	  //job.setKeyFieldComparatorOptions("-r");
        	  job.setNumReduceTasks(1);
              job.setJarByClass( this .getClass());
              job.setMapperClass( PageRankSorterMap.class);
              job.setReducerClass( PageRankSorterReduce.class);
              job.setOutputKeyClass( DoubleWritable.class);
              job.setOutputValueClass( Text.class);
              FileInputFormat.addInputPaths(job,  args[1]+".tmp/pagerank");
              FileOutputFormat.setOutputPath(job,  new Path(args[1]+".tmp/pageranksorted"));
	          if(job.waitForCompletion(true)==true) {
	        	  LOG.info("PageRank sorting finished");
	        	  FileSystem.get(getConf()).rename(new Path(args[1]+".tmp/pageranksorted"), new Path(args[1]));
	        	  FileSystem.get(getConf()).delete(new Path(args[1]+".tmp"), true);
	        	  return 0;
	          }
	        	
          }
      }
      return 1;
   }
   
   public static class LinkGraphMap extends Mapper<LongWritable ,  Text ,  Text ,  Text > {
      private static final Pattern titlePat = Pattern.compile("<title>.*?</title>");		
      private static final Pattern linkPat = Pattern .compile("\\[\\[.*?]\\]");
      private static final double d = 0.85;

      public void map( LongWritable offset,  Text lineText,  Context context)
        throws  IOException,  InterruptedException {
    	  // each line represents a wiki page content
    	  // sample line: <title>Page_Name</title>(other fields)<revision optionalAttr="val"><textoptionalAttr="val2">(Page body)</text></revision>
    	  // links to other Wikipedia articles are of the form “[[Name of other article]]”
    	  // if page links to itself, ignore
    	  
    	  String html = lineText.toString();
    	  
    	  // extract page title
    	  Matcher m = titlePat.matcher(html);
    	  if(m.find()) {
    		  String title = m.group().replace("<title>", "").replace("</title>", "");
    		  String urlList = "";
    		  
    		  int urlCount = 0;
    		  
    		  // extract page links of the form “[[Name of other article]]
    		  m = linkPat.matcher(html);
    		  while(m.find()) {
    			  String url = m.group().replace("[[", "").replace("]]", "");
    			  if(!url.isEmpty()) {
    				  if(url.indexOf(title+"#")!=0) { // if page links to itself, ignore
    				  	  String[] urlsplit = url.split("\\|");
    				  	  if(urlsplit.length<=2) {
    					  	if(urlsplit.length==2)    				  	 
    					  		url = urlsplit[0];

    					  	urlList += "\t"+url;
    					  	urlCount++;
    					}
    				  }
    			  }
    		  }
    		  // concatenate title+initial page rank+url count+linkess
			  // emit title+initial rank+linkees count+linkees
   	          context.write(new Text(title),new Text(String.valueOf(1-PageRank.d)+"\t"+String.valueOf(urlCount)+urlList));
    	  }
      }
   }

   public static class LinkGraphReduce extends Reducer<Text ,  Text ,  Text ,  Text > {
      @Override 
      public void reduce( Text key,  Iterable<Text > val,  Context context)
         throws IOException,  InterruptedException {
    	  // identity reducer
    	  for(Text t : val) {
    		  context.write(key,  t);
    	  }
      }
   }
   
   public static class PageRankMap extends Mapper<LongWritable ,  Text ,  Text ,  Text > {
	      public void map( LongWritable offset,  Text lineText,  Context context)
	        throws  IOException,  InterruptedException {
	    	  // each line represents title+initial rank+linkees count+linkees
	    	  	    	
	    	  // tokenize line
	    	  String line = lineText.toString();
	    	  String[] lineTokens = line.split("\t");
	    	  
	    	  if(lineTokens.length>3) { // this page has at least one outgoing link
	    		  int urlCount = Integer.parseInt(lineTokens[2]);
		    	  if(urlCount>0) {
		    		  // extract title
		    		  String title = lineTokens[0];
		    		  
		    		  // divide page rank by url count
		    		  double normalizedRank = Double.parseDouble(lineTokens[1])/urlCount;
		    		  
		    		  // for each linkee, emit linkee+normalized rank
		    		  for(int i=0; i<urlCount && i+3<lineTokens.length; i++) {
		    			  context.write(new Text(lineTokens[i+3]),new Text(String.valueOf(normalizedRank)));
		    		  }
		    	  }
	    	  }
	    	  // emit original line for updating page rank iteratively
	    	  context.write(new Text(lineTokens[0]),new Text(line.substring(line.indexOf('\t')+1)));	    	  
	      }   
   }


	   public static class PageRankReduce extends Reducer<Text ,  Text ,  Text ,  Text > {		  
	      @Override 
	      public void reduce( Text key,  Iterable<Text > val,  Context context)
	         throws IOException,  InterruptedException {
	    	  // value is either 
	    	  // 1- page as key with rank of source page as value
	    	  // 2- page as key with page title+current rank+linkees count+linkees list
	    	  double sum = 0.0;
	    	  String all = "";
	    	  for(Text v : val) {
	    		  String value = v.toString();
	    		  if(!value.contains("\t")) { // sum up
	    			  sum += Double.parseDouble(value);
	    		  }
	    		  else { // this is page rank with complete linkees list, store it for the future (must only happen once)
	    			  all = value;
	    		  }
	    	  }
	    	  
	    	  if(!all.isEmpty()) {
		    	  // now update page rank and emit
		    	  String[] allTokens = all.split("\t");
		    	  
		    	  // update page rank
		    	  allTokens[0] = String.valueOf((1-PageRank.d)+PageRank.d*sum);
		    	  String allnew = allTokens[0];
		    	  for(int i=1; i<allTokens.length; i++)
		    		  allnew += "\t" + allTokens[i];
		    	  
		    	  // emit original line again
		    	  context.write(key, new Text(allnew));
	    	  }
	      }
	   }
	   
	   public static class PageRankSorterMap extends Mapper<LongWritable ,  Text ,  DoubleWritable ,  Text > {
		      
		      public void map( LongWritable offset,  Text lineText,  Context context)
		        throws  IOException,  InterruptedException {
		    	  // each line represents title+final rank+linkees count+linkees
  	  	    	
		    	  // tokenize line
		    	  String[] lineTokens = lineText.toString().split("\t");
		    	  
		    	  // emit page rank as key and page title as value, negate to order descendingly
		    	  context.write(new DoubleWritable(-1*Double.parseDouble(lineTokens[1])),new Text(lineTokens[0]));
		      }
	   }
	   
	   public static class PageRankSorterReduce extends Reducer<DoubleWritable,  Text ,  Text ,  DoubleWritable> {		  
		      @Override 
		      public void reduce( DoubleWritable key,  Iterable<Text > val,  Context context)
		         throws IOException,  InterruptedException {
		    	  // key is page rank and value is list of pages having this page rank
		    	  // negate to recover from map negation
		    	  for(Text v : val) {
		    		  context.write(v,new DoubleWritable(-1*Double.parseDouble(key.toString())));
		    	  }
		      }
		   }
}
