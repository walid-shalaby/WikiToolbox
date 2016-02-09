#spark-submit SemanticsGenerator.py -p lines.txt -o msa_1000_gram100_seealsogram100_len0_seelen0_seealso_sup1_con500_alltext -c http://10.18.202.74:9091/solr/collection1/browse?q=&conceptsmethod=MSA_seealso&conceptsno=500&hmaxngrams=100&hseealsomaxngrams=100&hminwikilen=0&hminseealsolen=0&hminassocnt=1&hmaxhits=1000&hwikifield=alltext&hsim=cosine&hshowids=0&hshowpids=0&hshowweight=0&hshowassocounts=0&hshowtype=0&hshowdocno=0&hshowlen=0&hshowtable=0&hrelaxcache=on&hrelatednessexpr=&hexperin=&hexperout=&hrelaxcategories=&hrelaxsametitle=&hrelaxlistof=&hrelaxdisambig=&hrelaxner=&hwikiextraq=AND%2BNOT%2Btitle%253Alist*%2BAND%2BNOT%2Btitle%253Aindex*%2BAND%2BNOT%2Btitle%253A*disambiguation*&analytic=explore&wt=json
def process_line(line):
	import pycurl
	import StringIO
        import urllib
        import json

        line = line.split('\t')
        key = line[1]
        q = line[2]
	response = StringIO.StringIO()
	c = pycurl.Curl()
	c.setopt(c.URL, "http://10.18.202.74:9091/solr/collection1/browse?q="+urllib.quote_plus(q.encode('utf-8'))+"&conceptsmethod=MSA_seealso&conceptsno=500&hmaxngrams=100&hseealsomaxngrams=100&hminwikilen=0&hminseealsolen=0&hminassocnt=1&hmaxhits=1000&hwikifield=alltext&hsim=cosine&hshowids=0&hshowpids=0&hshowweight=0&hshowassocounts=0&hshowtype=0&hshowdocno=0&hshowlen=0&hshowtable=0&hrelaxcache=on&hrelatednessexpr=&hexperin=&hexperout=&hrelaxcategories=&hrelaxsametitle=&hrelaxlistof=&hrelaxdisambig=&hrelaxner=&hwikiextraq=AND%2BNOT%2Btitle%253Alist*%2BAND%2BNOT%2Btitle%253Aindex*%2BAND%2BNOT%2Btitle%253A*disambiguation*&analytic=explore&wt=json")
	c.setopt(c.WRITEFUNCTION, response.write)
	c.setopt(c.HTTPHEADER, ['Content-Type: application/json','Accept-Charset: UTF-8'])
	c.setopt(c.POSTFIELDS, '@request.json')
	c.perform()
	c.close()
	result = response.getvalue()
	response.close()
        if result.find('semantic_concepts')!=-1:
           concepts = json.loads(result)['semantic_concepts']
           result = key + '\t'
           for concept in concepts:
              result += concept + ','
           return result
        else:
           return result.replace("\n"," ")

def process(sc, input_dir, output_dir):
    inp = sc.textFile(input_dir)
    out = inp.map(lambda line: process_line(line))
    out.saveAsTextFile(output_dir)

def main():
    import io
    import os
    import sys
    import getopt
    from pyspark import SparkContext

    script_name = os.path.basename(sys.argv[0])

    sc = SparkContext(appName="WikiExtractor")

    try:
        long_opts = ['command', 'output=', 'input=']
        opts, args = getopt.gnu_getopt(sys.argv[1:], 'c:o:p:sv', long_opts)
    except getopt.GetoptError:
        show_usage(script_name)
        sys.exit(1)

    output_dir = '.'
    
    for opt, arg in opts:
        if opt in ('-c', '--command'):
            command = arg
        elif opt in ('-o', '--output'):
            output_dir = arg
        elif opt in ('-p', '--input'):
            input_dir = arg

    process(sc, input_dir, output_dir)
        
if __name__ == '__main__':
    main()
