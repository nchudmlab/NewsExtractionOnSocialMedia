package printer;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.sequences.DocumentReaderAndWriter;
import edu.stanford.nlp.util.Triple;

import java.util.List;


public class NERDemo {

  public static void testNER() throws Exception{
	  	System.out.println("ab");
	    String serializedClassifier = "classifiers/english.all.3class.distsim.crf.ser.gz";
	    AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifier(serializedClassifier);
	    

	      /* For the hard-coded String, it shows how to run it on a single
	         sentence, and how to do this and produce several formats, including
	         slash tags and an inline XML output format. It also shows the full
	         contents of the {@code CoreLabel}s that are constructed by the
	         classifier. And it shows getting out the probabilities of different
	         assignments and an n-best list of classifications with probabilities.
	      */

	      String[] example = {"Airbus subsidiary Premium AEROTEC has just reinforced its parent company’s commitment to 3D printing end parts, giving credence to the notion, posited by Airbus’s Peter Sander, that 2016 will see a big increase in 3D printing for industrial end products. The German aerospace manufacturer has just opened its first manufacturing facility for 3D printing titanium aircraft components, where it has begun serial production of metal 3D printed parts. Additionally, Premium AEROTEC is examining the use of Norsk Titanium‘s Rapid Plasma Deposition™ titanium parts for use in the production of the A350 XWB. At Premium AEROTEC’s Varel site, the company will be 3D printing complex parts for the A400M military transport aircraft. Among the parts to be produced will be a double-walled pipe elbow for the A400M’s fuel system, which, upon qualification for aviation standards, the firm will be able to supply to Airbus Defence and Space.  The pipe elbow was previously made up of individually cast parts subsequently welded together, but has been optimized to be 3D printed in a single job, saving the firm in terms of time and money needed to cast and weld the parts, as well as the equipment needed to perform these procedures.  At the moment, the newly modernized facility houses three metal 3D printers with a fourth on standby and a fifth to be added to the operation in May. Dr. Thomas Ehm, Chairman of the Board of Premium AEROTEC, said of the new metal 3D printing operation, “We want to push ahead for using 3D printing technology in aircraft manufacturing. This technology is breaking down the barriers of what can be produced, and when barriers are broken down, we need to be ready with our capacity for innovation to make the best possible use of the newly acquired freedom. We have to anticipate the possibilities that this will open up in our planning, and to make targeted use of them along the entire value creation chain.” Gerd Weber, head of the Varel site, added, “3D metal printing is enabling us to expand our capabilities at the Varel site and throughout the company to go beyond the existing processes. It will not replace our tried and tested processes, but it opens up as yet almost incalculable potential for us, especially when it comes to production times, flexibility in production and the weight of the components.” Premium AEROTEC has located itself at the center of a network of manufacturers, materials suppliers, and research institutions involved in the aerospace industry, generating greater opportunities for metal 3D printing for all stakeholders involved.  These partners include parts supplier MBFZ toolcraft, Hofmann Innovation Group, Concept Laser, and C.F.K innoshape, which will see the fertilization of a new metal 3D printing supply chain. They have hopes to industrialize laser sintering in order to get metal 3D printed parts into the aviation industry quickly. Norsk’s metal 3D printing process. Additionally, the subsidiary is collaborating with Norsk Titanium AS to test their Rapid Plasma Deposition in the production of near-net-shape parts, which are then machined by Premium AEROTEC. Norsk claims that they were able to supply the parts for the A350 XWB quickly, with Chief Commercial Officer Chet Fuller commenting, “We turned AEROTEC’s 3D CATIA files into flyable titanium parts in a matter of weeks under a cost reduction effort that could ultimately save Airbus $2-$3 million per aircraft.” We may still be years away from the 3D printed jetliner promised by Airbus a few years ago, but given the fact that it’s still January and we’ve written several stories about them, it sounds as though they’re making significant progress towards the use of 3D printing in their operations." };
	      for (String str : example) {
	        System.out.println(classifier.classifyToString(str));
	      }
	      System.out.println("---");

	      for (String str : example) {
	        // This one puts in spaces and newlines between tokens, so just print not println.
	        System.out.print(classifier.classifyToString(str, "slashTags", false));
	      }
	      System.out.println("---");

	      for (String str : example) {
	        // This one is best for dealing with the output as a TSV (tab-separated column) file.
	        // The first column gives entities, the second their classes, and the third the remaining text in a document
	        System.out.print(classifier.classifyToString(str, "tabbedEntities", false));
	      }
	      System.out.println("---");

	      for (String str : example) {
	        System.out.println(classifier.classifyWithInlineXML(str));
	      }
	      System.out.println("---");

	      for (String str : example) {
	        System.out.println(classifier.classifyToString(str, "xml", true));
	      }
	      System.out.println("---");

	      for (String str : example) {
	        System.out.print(classifier.classifyToString(str, "tsv", false));
	      }
	      System.out.println("---");

	      // This gets out entities with character offsets
	      int j = 0;
	      for (String str : example) {
	        j++;
	        List<Triple<String,Integer,Integer>> triples = classifier.classifyToCharacterOffsets(str);
	        for (Triple<String,Integer,Integer> trip : triples) {
	          System.out.printf("%s over character offsets [%d, %d) in sentence %d.%n",
	                  trip.first(), trip.second(), trip.third, j);
	        }
	      }
	      System.out.println("---");

	      // This prints out all the details of what is stored for each token
	      int i=0;
//	      for (String str : example) {
//	        for (List<CoreLabel> lcl : classifier.classify(str)) {
//	          for (CoreLabel cl : lcl) {
//	            System.out.print(i++ + ": ");
//	            System.out.println(cl.toShorterString());
//	          }
//	        }
//	      }

	      System.out.println("---");

  }
  public static void NERDemo(String[] args) throws Exception {

    String serializedClassifier = "classifiers/english.all.3class.distsim.crf.ser.gz";

    if (args.length > 1) {
      serializedClassifier = args[0];
    }

    AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifier(serializedClassifier);

    /* For either a file to annotate or for the hardcoded text example, this
       demo file shows several ways to process the input, for teaching purposes.
    */

    if (args.length > 1) {

      /* For the file, it shows (1) how to run NER on a String, (2) how
         to get the entities in the String with character offsets, and
         (3) how to run NER on a whole file (without loading it into a String).
      */

      String fileContents = IOUtils.slurpFile(args[1]);
      List<List<CoreLabel>> out = classifier.classify(fileContents);
      for (List<CoreLabel> sentence : out) {
        for (CoreLabel word : sentence) {
          System.out.print(word.word() + '/' + word.get(CoreAnnotations.AnswerAnnotation.class) + ' ');
        }
        System.out.println();
      }

      System.out.println("---");
      out = classifier.classifyFile(args[1]);
      for (List<CoreLabel> sentence : out) {
        for (CoreLabel word : sentence) {
          System.out.print(word.word() + '/' + word.get(CoreAnnotations.AnswerAnnotation.class) + ' ');
        }
        System.out.println();
      }

      System.out.println("---");
      List<Triple<String, Integer, Integer>> list = classifier.classifyToCharacterOffsets(fileContents);
      for (Triple<String, Integer, Integer> item : list) {
        System.out.println(item.first() + ": " + fileContents.substring(item.second(), item.third()));
      }
      System.out.println("---");
      System.out.println("Ten best entity labelings");
      DocumentReaderAndWriter<CoreLabel> readerAndWriter = classifier.makePlainTextReaderAndWriter();
      classifier.classifyAndWriteAnswersKBest(args[1], 10, readerAndWriter);

      System.out.println("---");
      System.out.println("Per-token marginalized probabilities");
      classifier.printProbs(args[1], readerAndWriter);

      // -- This code prints out the first order (token pair) clique probabilities.
      // -- But that output is a bit overwhelming, so we leave it commented out by default.
      // System.out.println("---");
      // System.out.println("First Order Clique Probabilities");
      // ((CRFClassifier) classifier).printFirstOrderProbs(args[1], readerAndWriter);

    } else {

      /* For the hard-coded String, it shows how to run it on a single
         sentence, and how to do this and produce several formats, including
         slash tags and an inline XML output format. It also shows the full
         contents of the {@code CoreLabel}s that are constructed by the
         classifier. And it shows getting out the probabilities of different
         assignments and an n-best list of classifications with probabilities.
      */

      String[] example = {"Good afternoon Rajat Raina, how are you today?",
                          "I go to school at Stanford University, which is located in California." };
      for (String str : example) {
        System.out.println(classifier.classifyToString(str));
      }
      System.out.println("---");

      for (String str : example) {
        // This one puts in spaces and newlines between tokens, so just print not println.
        System.out.print(classifier.classifyToString(str, "slashTags", false));
      }
      System.out.println("---");

      for (String str : example) {
        // This one is best for dealing with the output as a TSV (tab-separated column) file.
        // The first column gives entities, the second their classes, and the third the remaining text in a document
        System.out.print(classifier.classifyToString(str, "tabbedEntities", false));
      }
      System.out.println("---");

      for (String str : example) {
        System.out.println(classifier.classifyWithInlineXML(str));
      }
      System.out.println("---");

      for (String str : example) {
        System.out.println(classifier.classifyToString(str, "xml", true));
      }
      System.out.println("---");

      for (String str : example) {
        System.out.print(classifier.classifyToString(str, "tsv", false));
      }
      System.out.println("---");

      // This gets out entities with character offsets
      int j = 0;
      for (String str : example) {
        j++;
        List<Triple<String,Integer,Integer>> triples = classifier.classifyToCharacterOffsets(str);
        for (Triple<String,Integer,Integer> trip : triples) {
          System.out.printf("%s over character offsets [%d, %d) in sentence %d.%n",
                  trip.first(), trip.second(), trip.third, j);
        }
      }
      System.out.println("---");

      // This prints out all the details of what is stored for each token
      int i=0;
      for (String str : example) {
        for (List<CoreLabel> lcl : classifier.classify(str)) {
          for (CoreLabel cl : lcl) {
            System.out.print(i++ + ": ");
            System.out.println(cl.toShorterString());
          }
        }
      }

      System.out.println("---");

    }
  }

}
