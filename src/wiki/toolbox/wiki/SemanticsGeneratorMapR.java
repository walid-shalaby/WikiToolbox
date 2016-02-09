package wiki.toolbox.wiki;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
//import org.apache.solr.client.solrj.SolrServerException;
//import org.apache.solr.client.solrj.impl.HttpSolrServer;
//import org.apache.solr.common.SolrInputDocument;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.http.client.utils.URLEncodedUtils;

import wiki.toolbox.semantic.SemanticSearchConfigParams;

public class SemanticsGeneratorMapR extends Configured implements Tool {

   public static void main( String[] args) throws  Exception {
      int res  = ToolRunner.run( new SemanticsGeneratorMapR(), args);
      System.exit(res);
   }

   public int run( String[] args) throws  Exception {
      Job job  = Job.getInstance(getConf(), "semanticgenerator");
      job.setJarByClass(this.getClass());

      getConf().set("args", StringUtils.join(" ",args));
      
      FileInputFormat.addInputPaths(job, args[0]);
      FileOutputFormat.setOutputPath(job, new Path(args[1]));
      job.setMapperClass(SemanticsGeneratorMap.class);
      job.setReducerClass(SemanticsGeneratorReduce.class);
      job.setOutputKeyClass(Text.class);
      job.setOutputValueClass(Text.class);

      return job.waitForCompletion(true)  ? 0 : 1;
   }
   
   public static class SemanticsGeneratorMap extends Mapper<LongWritable ,  Text ,  Text ,  Text > {
      public void map(LongWritable offset,  Text lineText,  Context context)
        throws  IOException,  InterruptedException {
         /*
    	 String opts = context.getConfiguration().get("args");
    	 SemanticSearchConfigParams params;
    	 if(params.parseOpts(opts.split(" "))==true) {
    		 ArrayList<String> cmd = new ArrayList<String>();
    		 cmd.add(params.wikiUrl);
    		 cmd.add("q="+URLEncoder.encode(lineText.toString()));
    		 cmd.add("conceptsmethod="+URLEncoder.encode(Enums. e_Method));
    		 "http://10.18.203.79:9091/solr/collection1/browse?q=&conceptsmethod=MSA_seealso&conceptsno=20&hmaxngrams=3&hseealsomaxngrams=3&hminwikilen=0&hminseealsolen=0&hminassocnt=1&hmaxhits=1000&hwikifield=text&hsim=cosine&hshowids=0&hshowpids=0&hshowweight=0&hshowassocounts=0&hshowtype=0&hshowdocno=0&hshowlen=0&hshowtable=0&hrelaxcache=on&hrelatednessexpr=&hexperin=&hexperout=&hrelaxcategories=&hrelaxsametitle=&hrelaxlistof=&hrelaxdisambig=&hrelaxner=&hwikiextraq=AND%2BNOT%2Btitle%253Alist*%2BAND%2BNOT%2Btitle%253Aindex*%2BAND%2BNOT%2Btitle%253A*disambiguation*&analytic=explore&wt=json"
    	 }
         // each line represents a patent in the USPTO xml format
         String in  = lineText.toString();

         // parse the xml according to its DTD version and extract values of target index fields from it
         Patent patent = PatentTransformer.transform(patentxml);
         if(patent.getId().compareTo("")!=0) { // parsed successfully
        	 // emit id and single format xml
        	 context.write(new Text(patent.getId()), new Text(patent.toXMLLine(true)));
         }*/         
      }
   }

   public static class SemanticsGeneratorReduce extends Reducer<Text ,  Text ,  Text ,  Text> {
      @Override 
      public void reduce( Text id,  Iterable<Text> parsed,  Context context)
         throws IOException,  InterruptedException {
    	  // each id is unique and will be associated with only one xml
         for ( Text parsedpatent : parsed) {
        	 context.write(id,  parsedpatent);        	 
         }
      }
   }
}


