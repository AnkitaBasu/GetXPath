import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.TreeSet;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX handler that creates and prints XPath expressions for each element encountered.
 *
 * The algorithm is not infallible, if elements appear on different levels in the hierarchy.
 * Something like the following is an example:
 * - <elemA/>
 * - <elemB/>
 * - <elemC>
 * -     <elemD/>
 * - </elemC>
 *
 * will report
 *
 * //elemA
 * //elemB
 * //elemC/elemD
 *
 * It also ignores namespaces, and thus treats <foo:elemA> the same as <bar:elemA>.
 */

public class GenerateXPathFromXml extends DefaultHandler {

	/** The tag count. */
	// map of all encountered tags and their running count
	private Map<String, Integer> tagCount;

	/** The tags. */
	// keep track of the succession of elements
	private Stack<String> tags;

	/** The last closed tag. */
	// set to the tag name of the recently closed tag
	String lastClosedTag;

	/** The li. */
	public static List<String> li = new ArrayList<String>();

	/** The set. */
	public static TreeSet<String> set=new TreeSet<String>();  


	/**
	 * Construct the XPath expression.
	 *
	 * @return the current X path
	 */
	private String getCurrentXPath() {
		String str = "//";
		boolean first = true;
		for (String tag : tags) {
			if (first)
				str = str + tag;
			else
				str = str + "/" + tag;
			first = false;
		}
		return str;
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	@Override
	public void startDocument() throws SAXException {
		tags = new Stack();
		tagCount = new HashMap<String, Integer>();
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement (String namespaceURI, String localName, String qName, Attributes atts)
			throws SAXException
	{
		boolean isRepeatElement = false;

		if (tagCount.get(localName) == null) {
			tagCount.put(localName, 0);
		} else {
			tagCount.put(localName, 1 + tagCount.get(localName));
		}

		if (lastClosedTag != null) {
			// an element was recently closed ...
			if (lastClosedTag.equals(localName)) {
				// ... and it's the same as the current one
				isRepeatElement = true;
			} else {
				// ... but it's different from the current one, so discard it
				tags.pop();
			}
		}

		// if it's not the same element, add the new element and zero count to list
		if (! isRepeatElement) {
			tags.push(localName);
		}
		li.add(getCurrentXPath());


		lastClosedTag = null;
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement (String uri, String localName, String qName) throws SAXException {
		// if two tags are closed in succession (without an intermediate opening tag),
		// then the information about the deeper nested one is discarded
		if (lastClosedTag != null) {
			tags.pop();
		}
		lastClosedTag = localName;
	}

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws Exception the exception
	 */
	public static void main (String[] args) throws Exception {

		Scanner scan = new Scanner(System.in);
		String inputFilePath;
		String outFilePath;
		try{
			System.out.println("This is a Utility to generate xPath for all elements from sample xml\n");    
			System.out.println("\nProvide sample xml file path Ex- C:\\Test\\test.xml\n");
			inputFilePath = scan.next();
			System.out.println("\nProvide outfile path Ex- C:\\Test\\Result.txt\n");
			outFilePath = scan.next();
		}
		finally
		{
			scan.close();
		}

		if (inputFilePath.length() < 1 || !inputFilePath.matches("(.*).xml")) {
			System.err.println("Usage: GenerateXPathFromXml <file.xml>");
			System.exit(1);
		}
		if (outFilePath.length() < 1 || !outFilePath.matches("(.*).txt")) {
			System.err.println("Usage: GenerateXPathFromXml <file.txt>");
			System.exit(1);
		}

		// Create a JAXP SAXParserFactory and configure it
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		spf.setValidating(false);

		// Create a JAXP SAXParser
		SAXParser saxParser = spf.newSAXParser();

		// Get the encapsulated SAX XMLReader
		XMLReader xmlReader = saxParser.getXMLReader();

		// Set the ContentHandler of the XMLReader
		xmlReader.setContentHandler(new GenerateXPathFromXml());

		if (File.separatorChar != '/') {
			inputFilePath = inputFilePath.replace(File.separatorChar, '/');
		}
		if (!inputFilePath.startsWith("/")) {
			inputFilePath = "/" + inputFilePath;
		}

		// Tell the XMLReader to parse the XML document
		xmlReader.parse("file:"+inputFilePath);

		GenerateXPathFromXml s = new GenerateXPathFromXml();

		int i =0;

		s.check(li.get(i), li.get(i+1), i+1, outFilePath);

	}

	/**
	 * Check.
	 *
	 * @param current the current
	 * @param next the next
	 * @param i the i
	 * @param out the out
	 * @throws FileNotFoundException the file not found exception
	 */
	public void check (String current, String next, int i, String out) throws FileNotFoundException  {

		if ( next.split("/").length > current.split("/").length)
		{
			current = next;
			next = li.get(i+1);
			check(current, next, ++i, out);
		}
		else
		{
			set.add(current);

			if(i== li.size()-1 && next.split("/").length == current.split("/").length)
			{

				set.add(next);
				File file = new File(out);
				FileOutputStream fos = new FileOutputStream(file);

				// Create new print stream for file.
				PrintStream ps = new PrintStream(fos);

				// Set file print stream.
				System.setOut(ps);
				Iterator<String> itr=set.iterator();  
				while(itr.hasNext()){  

					System.out.println(itr.next());  
				}  
				System.setOut(System.err);
				System.err.println("\n****************************Done************************************");
				System.exit(0);
			}
			else
			{
				current = next;
				next = li.get(i+1);
				check(current, next, ++i,out);
			}

		}


	}


}