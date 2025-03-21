package zzz;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import chav1961.purelib.basic.AndOrTree;
import chav1961.purelib.basic.BKTree;
import chav1961.purelib.basic.CharUtils;
import chav1961.purelib.basic.interfaces.SyntaxTreeInterface;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;


public class CorporaLoader {
	public static void main(final String[] args) throws IOException, ParserConfigurationException, SAXException {
		final BKTree<char[], Lemma>	tree = new BKTree<>(char[].class, CorporaLoader::calculate);
		
		try(final InputStream	is = new FileInputStream("c:/tmp/dict.opcorpora.xml")) {
			final SAXParserFactory 	factory = SAXParserFactory.newInstance();
			final SAXParser 		saxParser = factory.newSAXParser();
			final CorporaHandler	ch = new CorporaHandler(); 

			final long	start1 = System.currentTimeMillis();
			
			saxParser.parse(is, ch);
			System.err.println("Loading completed "+(System.currentTimeMillis() - start1));
			final long	start2 = System.currentTimeMillis();
			
			ch.forms.walk(new SyntaxTreeInterface.Walker<CorporaLoader.Lemma>() {
//				int	index = 0;
				@Override
				public boolean process(final char[] name, final int len, final long id, final Lemma cargo) {
					tree.add(cargo.text, cargo);
//					System.err.print(".");
//					if (index++ % 100 == 0) {
//						System.err.println(index);
//					}
					return true;
				}
			});
			System.err.println("BK-tree completed "+(System.currentTimeMillis() - start2));
		}
		final ForkJoinPool	fjp = ForkJoinPool.commonPool();
		
		try(final Reader			rdr = new InputStreamReader(System.in);
			final BufferedReader	brdr = new BufferedReader(rdr)) {
			String	line;
			
			while ((line = brdr.readLine()) != null) {
				final String[]	words = line.split("\\s+");
				final String[]	result = new String[words.length];
				final Parser	p = new Parser(tree, words, result, 0, words.length-1);
				
				fjp.invoke(p);
				for(String item : result) {
	        		System.err.println(item);
				}
			}
		} finally {
			fjp.shutdownNow();
		} 
	}
	
	private static int calculate(final char[] left, final char[] right) {
		return CharUtils.calcLevenstain(left, right).distance;
	}

	private static class Parser extends RecursiveAction {
		private final BKTree<char[], Lemma> tree;
		private final String[]	source;
		private final String[]	result;
		private final int		from;
		private final int		to;
		
		private Parser(final BKTree<char[], Lemma> tree, final String[] source, final String[] result, final int from, final int to) {
			this.tree = tree;
			this.source = source;
			this.result = result;
			this.from = from;
			this.to = to;
		}

		@Override
		protected void compute() {
//			System.err.println("Calc: "+from+"/"+to);
			if (from == to) {
				result[from] = process(source[from]);
			}
			else {
				final int		mid = (from + to) / 2;
				final Parser	left = new Parser(tree, source, result, from, mid); 
				final Parser	right = new Parser(tree, source, result, mid+1, to);
				
				left.fork();
				right.fork();
				left.join();
				right.join();
			}
		}

		private String process(final String word) {
        	final char[]	content = word.toCharArray();
        	
        	if (tree.contains(content)) {
        		return word+" OK";
        	}
        	else {
        		final List<String>	available = new ArrayList<>();
        		
        		tree.walk(content, Math.max(1, content.length/3), (c,m,l)->{
        			available.add(new String(c));
        			return true;
        		});
        		return word+" failed, available are "+available;
        	}
		}
	}
	
	private static class CorporaHandler extends DefaultHandler {
	    private static final String	DICTIONARY = "dictionary";
	    private static final String	GRAMMEMES = "grammemes";
	    private static final String	GRAMMEME = "grammeme";
	    private static final String	ATTR_PARENT = "parent";
	    private static final String	NAME = "name";
	    private static final String	ALIAS = "alias";
	    private static final String	DESCRIPTION = "description";
	    private static final String	RESTRICTIONS = "restrictions";
	    private static final String	RESTRICTION = "restr";
	    private static final String	ATTR_TYPE = "type";
	    private static final String	ATTR_AUTO = "auto";
	    private static final String	LEFT = "left";
	    private static final String	LEFT_TYPE = "left_type";
	    private static final String	RIGHT = "right";
	    private static final String	RIGHT_TYPE = "right_type";
	    private static final String	LEMMATA = "lemmata";
	    private static final String	LEMMA = "lemma";
	    private static final String	ATTR_ID = "id";
	    private static final String	ATTR_REV = "rev";
	    private static final String	ATTR_T = "t";
	    private static final String	ATTR_V = "v";
	    private static final String	L = "l";
	    private static final String	G = "g";
	    private static final String	F = "f";
	    private static final String	LINK_TYPES = "link_types";
	    private static final String	LINK_TYPE = "type";
	    private static final String	LINKS = "links";
	    private static final String	LINK = "link";

	    private static enum State {
	    	NOWHERE,
	    	IN_DOCUMENT,
	    	IN_GRAMMEMES,
	    	IN_GRAMMEME,
	    	IN_RESTRICTIONS,
	    	IN_RESTRICTION,
	    	IN_LEMMAS,
	    	IN_LEMMA,
	    	IN_LINKTYPES,
	    	IN_LINKS,
	    }
	    
	    private final StringBuilder 		elementValue = new StringBuilder();
	    private final Map<String, Grammema>	grammemas = new HashMap<>();
	    private final List<Restriction>		restrictions = new ArrayList<>();
	    private final SyntaxTreeInterface<Lemma>	lemmas = new AndOrTree<>();
	    private final SyntaxTreeInterface<Lemma>	forms = new AndOrTree<>();
	    private final List<Link>			links = new ArrayList<>();
	    private final List<Grammema>		attrs = new ArrayList<>();
	    private final Properties			props = new Properties();
	    private char[]	name;
	    private int		id, rev;
	    private Lemma	lastLemma;
	    private State	currentState = State.NOWHERE; 

	    @Override
	    public void startDocument() throws SAXException {
	    }

	    @Override
	    public void startElement(final String uri, final String lName, final String qName, final Attributes attr) throws SAXException {
	    	switch (qName) {
	    		case DICTIONARY		:
	    	    	currentState = State.IN_DOCUMENT;
	    	    	break;
	    		case GRAMMEMES 		:
	    			if (currentState == State.IN_DOCUMENT) {
	    				currentState = State.IN_GRAMMEMES;
	    			}
	    			else {
	    				throw new UnsupportedOperationException("Unsupported tag nesting: state="+currentState+", tag="+qName);
	    			}
	    			break;
	    		case GRAMMEME 		:
	    			if (currentState == State.IN_GRAMMEMES) {
	    				props.setProperty(ATTR_PARENT, attr.getValue(ATTR_PARENT));
	    				currentState = State.IN_GRAMMEME;
	    			}
	    			else {
	    				throw new UnsupportedOperationException("Unsupported tag nesting: state="+currentState+", tag="+qName);
	    			}
	    			break;
	    		case NAME : case ALIAS : case DESCRIPTION :
	    			elementValue.setLength(0);
	    			break;
	    		case RESTRICTIONS	:
	    			if (currentState == State.IN_DOCUMENT) {
	    				currentState = State.IN_RESTRICTIONS;
	    			}
	    			else {
	    				throw new UnsupportedOperationException("Unsupported tag nesting: state="+currentState+", tag="+qName);
	    			}
	    			break;
	    		case RESTRICTION	:
	    			if (currentState == State.IN_RESTRICTIONS) {
	    				props.setProperty(ATTR_TYPE, attr.getValue(ATTR_TYPE));
	    				props.setProperty(ATTR_AUTO, attr.getValue(ATTR_AUTO));
	    				currentState = State.IN_RESTRICTION;
	    			}
	    			else {
	    				throw new UnsupportedOperationException("Unsupported tag nesting: state="+currentState+", tag="+qName);
	    			}
	    			break;
	    		case LEFT			:
	    			props.setProperty(LEFT_TYPE, attr.getValue(ATTR_TYPE));
	    			elementValue.setLength(0);
	    			break;
	    		case RIGHT			:
	    			props.setProperty(RIGHT_TYPE, attr.getValue(ATTR_TYPE));
	    			elementValue.setLength(0);
	    			break;
	    		case LEMMATA		:
	    			if (currentState == State.IN_DOCUMENT) {
	    				currentState = State.IN_LEMMAS;
	    			}
	    			else {
	    				throw new UnsupportedOperationException("Unsupported tag nesting: state="+currentState+", tag="+qName);
	    			}
	    			break;
	    		case LEMMA			:
	    			if (currentState == State.IN_LEMMAS) {
	    				id = Integer.valueOf(attr.getValue(ATTR_ID));
	    				rev = Integer.valueOf(attr.getValue(ATTR_REV));
	    				currentState = State.IN_LEMMA;
	    			}
	    			else {
	    				throw new UnsupportedOperationException("Unsupported tag nesting: state="+currentState+", tag="+qName);
	    			}
	    			break;
	    		case L				:
	    			name = attr.getValue(ATTR_T).toCharArray();
	    			attrs.clear();
	    			break;
	    		case G				:
	    			attrs.add(grammemas.get(attr.getValue(ATTR_V)));
	    			break;
	    		case F				:
	    			name = attr.getValue(ATTR_T).toCharArray();
	    			attrs.clear();
	    			break;
	    		case LINK_TYPES		:
	    			if (currentState == State.IN_DOCUMENT) {
	    				currentState = State.IN_LINKTYPES;
	    			}
	    			else {
	    				throw new UnsupportedOperationException("Unsupported tag nesting: state="+currentState+", tag="+qName);
	    			}
	    			break;
	    		case LINK_TYPE		:
	    			id = Integer.valueOf(attr.getValue(ATTR_ID));
	    			elementValue.setLength(0);
	    			break;
	    		case LINKS			:
	    			if (currentState == State.IN_DOCUMENT) {
	    				currentState = State.IN_LINKS;
	    			}
	    			else {
	    				throw new UnsupportedOperationException("Unsupported tag nesting: state="+currentState+", tag="+qName);
	    			}
	    			break;
	    		case LINK			:
	    			links.add(new Link(Integer.valueOf(attr.getIndex(ATTR_ID)),
		    					LinkType.of(Integer.valueOf(attr.getIndex(ATTR_TYPE))),
		    					Integer.valueOf(attr.getIndex(LEFT)),
		    					Integer.valueOf(attr.getIndex(RIGHT))
	    					));
	    			break;
	    		default :
	    			throw new UnsupportedOperationException("Tag ["+qName+"] is not supported yet");
	    	}
	    }

	    @Override
	    public void characters(final char[] ch, final int start, final int length) throws SAXException {
	    	elementValue.append(ch, start, length);
	    }
	    
	    @Override
	    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
	    	switch (qName) {
	    		case DICTIONARY		:
	    	    	currentState = State.NOWHERE;
	    	    	break;
	    		case GRAMMEMES 		:
	    			if (currentState == State.IN_GRAMMEMES) {
	    				currentState = State.IN_DOCUMENT;
	    			}
	    			else {
	    				throw new UnsupportedOperationException("Unsupported tag nesting: state="+currentState+", tag="+qName);
	    			}
	    			break;
	    		case GRAMMEME 		:
	    			if (currentState == State.IN_GRAMMEME) {
	    				grammemas.put(props.getProperty(NAME), 
    								new Grammema(grammemas.get(props.getProperty(ATTR_PARENT)), 
    											props.getProperty(NAME), 
    											props.getProperty(ALIAS), 
    											props.getProperty(DESCRIPTION)
    								)
	    				);
	    				currentState = State.IN_GRAMMEMES;
	    			}
	    			else {
	    				throw new UnsupportedOperationException("Unsupported tag nesting: state="+currentState+", tag="+qName);
	    			}
	    			break;
	    		case NAME : case ALIAS : case DESCRIPTION :
	    			props.setProperty(qName, elementValue.toString());
	    			break;
	    		case RESTRICTIONS	:
	    			if (currentState == State.IN_RESTRICTIONS) {
	    				currentState = State.IN_DOCUMENT;
	    			}
	    			else {
	    				throw new UnsupportedOperationException("Unsupported tag nesting: state="+currentState+", tag="+qName);
	    			}
	    			break;
	    		case RESTRICTION	:
	    			if (currentState == State.IN_RESTRICTION) {
	    				restrictions.add(new Restriction(Restriction.RestrictionType.of(props.getProperty(ATTR_TYPE)), 
		    						grammemas.get(props.getProperty(LEFT)), 
		    						LemmaType.of(props.getProperty(LEFT_TYPE)), 
		    						grammemas.get(props.getProperty(RIGHT)), 
		    						LemmaType.of(props.getProperty(RIGHT_TYPE)),
		    						Integer.valueOf(props.getProperty(ATTR_AUTO)))
	    				);
	    				currentState = State.IN_RESTRICTIONS;
	    			}
	    			else {
	    				throw new UnsupportedOperationException("Unsupported tag nesting: state="+currentState+", tag="+qName);
	    			}
	    			break;
	    		case LEFT			:
    				props.setProperty(LEFT, elementValue.toString());
	    			break;
	    		case RIGHT			:
    				props.setProperty(RIGHT, elementValue.toString());
	    			break;
	    		case LEMMATA		:
	    			if (currentState == State.IN_LEMMAS) {
	    				currentState = State.IN_DOCUMENT;
	    			}
	    			else {
	    				throw new UnsupportedOperationException("Unsupported tag nesting: state="+currentState+", tag="+qName);
	    			}
	    			break;
	    		case LEMMA			:
	    			if (currentState == State.IN_LEMMA) {
	    				currentState = State.IN_LEMMAS;
	    			}
	    			else {
	    				throw new UnsupportedOperationException("Unsupported tag nesting: state="+currentState+", tag="+qName);
	    			}
	    			break;
	    		case L				:
	    			final long	lemmaId = lemmas.seekName(name, 0, name.length);
	    			
	    			if (lemmaId >= 0) {
		    			lastLemma = new Lemma(null, id, rev, LemmaType.LEMMA, name, attrs.toArray(new Grammema[attrs.size()]));
		    			
		    			lemmas.getCargo(lemmaId).chain = lastLemma;
	    			}
	    			else {
	    				lemmas.placeName(name, 0, name.length, id, 
	    						lastLemma = new Lemma(null, id, rev, LemmaType.LEMMA, name, attrs.toArray(new Grammema[attrs.size()]))
    					);
	    			}
	    			break;
	    		case G				:
	    			break;
	    		case F				:
	    			final long	formId = forms.seekName(name, 0, name.length);
	    			
	    			if (formId >= 0) {
	    				final Lemma	prev = forms.getCargo(formId);
		    			final Lemma	lastForm = new Lemma(lastLemma, id, rev, LemmaType.FORM, prev.text, attrs.toArray(new Grammema[attrs.size()]));
		    			
		    			prev.chain = lastForm;
	    			}
	    			else {
	    				forms.placeName(name, 0, name.length, id, 
	    						new Lemma(lastLemma, id, rev, LemmaType.FORM, name, attrs.toArray(new Grammema[attrs.size()]))
    					);
	    			}
	    			break;
	    		case LINK_TYPES		:
	    			if (currentState == State.IN_LINKTYPES) {
	    				currentState = State.IN_DOCUMENT;
	    			}
	    			else {
	    				throw new UnsupportedOperationException("Unsupported tag nesting: state="+currentState+", tag="+qName);
	    			}
	    			break;
	    		case LINK_TYPE		:
	    			if (id != LinkType.of(elementValue.toString()).getId()) {
		    			throw new UnsupportedOperationException("Link type ["+elementValue.toString()+"] incompatible with internal settings");
	    			}
	    			break;
	    		case LINKS			:
	    			if (currentState == State.IN_LINKS) {
	    				currentState = State.IN_DOCUMENT;
	    			}
	    			else {
	    				throw new UnsupportedOperationException("Unsupported tag nesting: state="+currentState+", tag="+qName);
	    			}
	    			break;
	    		case LINK			:
	    			break;
	    		default :
	    			throw new UnsupportedOperationException("Tag ["+qName+"] is not supported yet");
	    	}
	    }

	    @Override
	    public void endDocument() throws SAXException {
	    }
	}
	
	public static class Grammema {
		private final Grammema	parent;
		private final String	name;
		private final String	alias;
		private final String	description;

		public Grammema(final Grammema parent, final String name, final String alias, final String description) {
			this.parent = parent;
			this.name = name;
			this.alias = alias;
			this.description = description;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((alias == null) ? 0 : alias.hashCode());
			result = prime * result + ((description == null) ? 0 : description.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((parent == null) ? 0 : parent.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Grammema other = (Grammema) obj;
			if (alias == null) {
				if (other.alias != null) return false;
			} else if (!alias.equals(other.alias)) return false;
			if (description == null) {
				if (other.description != null) return false;
			} else if (!description.equals(other.description)) return false;
			if (name == null) {
				if (other.name != null) return false;
			} else if (!name.equals(other.name)) return false;
			if (parent == null) {
				if (other.parent != null) return false;
			} else if (!parent.equals(other.parent)) return false;
			return true;
		}

		@Override
		public String toString() {
			return "Grammema [parent=" + parent + ", name=" + name + ", alias=" + alias + ", description=" + description + "]";
		}
	}

	public static enum LemmaType {
		LEMMA("lemma"),
		FORM("form");
		
		private final String	abbr;
		
		private LemmaType(final String abbr) {
			this.abbr = abbr;
		}
		
		public String getAbbreviature() {
			return abbr;
		}
		
		public static LemmaType of(final String abbr) {
			if (abbr == null) {
				throw new NullPointerException("Abbreviature can't be null");
			}
			else {
				for(LemmaType item : values()) {
					if (item.getAbbreviature().equals(abbr)) {
						return item;
					}
				}
				throw new IllegalArgumentException("Abbreviature ["+abbr+"] not found in the enum");
			}
		}
	}
	
	public static class Restriction {
		public static enum RestrictionType {
			OBLIGATORY("obligatory"),
			MAYBE("maybe"),
			FORBIDDEN("forbidden");
			
			private final String	abbr;
			
			private RestrictionType(final String abbr) {
				this.abbr = abbr;
			}
			
			public String getAbbreviature() {
				return abbr;
			}
			
			public static RestrictionType of(final String abbr) {
				if (abbr == null) {
					throw new NullPointerException("Abbreviature can't be null");
				}
				else {
					for(RestrictionType item : values()) {
						if (item.getAbbreviature().equals(abbr)) {
							return item;
						}
					}
					throw new IllegalArgumentException("Abbreviature ["+abbr+"] not found in the enum");
				}
			}
		}

		private final RestrictionType	type; 
		private final Grammema			left;
		private final LemmaType			leftType;
		private final Grammema			right;
		private final LemmaType			rightType;
		private final int				auto;
		
		public Restriction(final RestrictionType type, final Grammema left, final LemmaType leftType, final Grammema right, final LemmaType rightType, final int auto) {
			this.type = type;
			this.left = left;
			this.leftType = leftType;
			this.right = right;
			this.rightType = rightType;
			this.auto = auto;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + auto;
			result = prime * result + ((left == null) ? 0 : left.hashCode());
			result = prime * result + ((leftType == null) ? 0 : leftType.hashCode());
			result = prime * result + ((right == null) ? 0 : right.hashCode());
			result = prime * result + ((rightType == null) ? 0 : rightType.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Restriction other = (Restriction) obj;
			if (auto != other.auto) return false;
			if (left == null) {
				if (other.left != null) return false;
			} else if (!left.equals(other.left)) return false;
			if (leftType != other.leftType) return false;
			if (right == null) {
				if (other.right != null) return false;
			} else if (!right.equals(other.right)) return false;
			if (rightType != other.rightType) return false;
			if (type != other.type) return false;
			return true;
		}

		@Override
		public String toString() {
			return "Restriction [type=" + type + ", left=" + left + ", leftType=" + leftType + ", right=" + right
					+ ", rightType=" + rightType + ", auto=" + auto + "]";
		}
	}
	
	public static class Lemma {
		public Lemma				chain = null;
		private final Lemma			parent;
		private final long			id;
		private final long			rev;
		private final LemmaType		type;
		private final char[]		text;
		private final Grammema[]	attrs;
		
		public Lemma(final Lemma parent, final long id, final long rev, final LemmaType type, final char[] text, final Grammema[] attrs) {
			this.parent = parent;
			this.id = id;
			this.rev = rev;
			this.type = type;
			this.text = text;
			this.attrs = attrs;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(attrs);
			result = prime * result + (int) (id ^ (id >>> 32));
			result = prime * result + ((parent == null) ? 0 : parent.hashCode());
			result = prime * result + (int) (rev ^ (rev >>> 32));
			result = prime * result + ((text == null) ? 0 : Arrays.hashCode(text));
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Lemma other = (Lemma) obj;
			if (!Arrays.equals(attrs, other.attrs)) return false;
			if (id != other.id) return false;
			if (parent == null) {
				if (other.parent != null) return false;
			} else if (!parent.equals(other.parent)) return false;
			if (rev != other.rev) return false;
			if (text == null) {
				if (other.text != null) return false;
			} else if (!Arrays.equals(text, other.text)) return false;
			if (type != other.type) return false;
			return true;
		}

		@Override
		public String toString() {
			return "Lemma [chain=" + chain + ", parent=" + parent + ", id=" + id + ", rev=" + rev + ", type=" + type
					+ ", text=" + Arrays.toString(text) + ", attrs=" + Arrays.toString(attrs) + "]";
		}
	}
	
	public static enum LinkType {
		ADJF_ADJS(1, "ADJF-ADJS"),
		ADJF_COMP(2, "ADJF-COMP"),
		INFN_VERB(3, "INFN-VERB"),
		INFN_PRTF(4, "INFN-PRTF"),
		INFN_GRND(5, "INFN-GRND"),
		PRTF_PRTS(6, "PRTF-PRTS"),
		NAME_PATR(7, "NAME-PATR"),
		PATR_MASC_PATR_FEMN(8, "PATR_MASC-PATR_FEMN"),
		SURN_MASC_SURN_FEMN(9, "SURN_MASC-SURN_FEMN"),
		SURN_MASC_SURN_PLUR(10, "SURN_MASC-SURN_PLUR"),
		PERF_IMPF(11, "PERF-IMPF"),
		ADJF_SUPR_ejsh(12, "ADJF-SUPR_ejsh"),
		PATR_MASC_FORM_PATR_MASC_INFR(13, "PATR_MASC_FORM-PATR_MASC_INFR"),
		PATR_FEMN_FORM_PATR_FEMN_INFR(14, "PATR_FEMN_FORM-PATR_FEMN_INFR"),
		ADJF_eish_SUPR_nai_eish(15, "ADJF_eish-SUPR_nai_eish"),
		ADJF_SUPR_ajsh(16, "ADJF-SUPR_ajsh"),
		ADJF_aish_SUPR_nai_aish(17, "ADJF_aish-SUPR_nai_aish"),
		ADJF_SUPR_suppl(18, "ADJF-SUPR_suppl"),
		ADJF_SUPR_nai(19, "ADJF-SUPR_nai"),
		ADJF_SUPR_slng(20, "ADJF-SUPR_slng"),
		FULL_CONTRACTED(21, "FULL-CONTRACTED"),
		NORM_ORPHOVAR(22, "NORM-ORPHOVAR"),
		CARDINAL_ORDINAL(23, "CARDINAL-ORDINAL"),
		SBST_MASC_SBST_FEMN(24, "SBST_MASC-SBST_FEMN"),
		SBST_MASC_SBST_PLUR(25, "SBST_MASC-SBST_PLUR"),
		ADVB_COMP(26, "ADVB-COMP"),
		ADJF_TEXT_ADJF_NUMBER(27, "ADJF_TEXT-ADJF_NUMBER");
		
		private final int		id;
		private final String	abbr;
		
		private LinkType(final int id, final String abbr) {
			this.id = id;
			this.abbr = abbr;
		}
		
		public int getId() {
			return id;
		}
		
		public String getAbbreviature() {
			return abbr;
		}

		public static LinkType of(final String abbr) {
			if (abbr == null) {
				throw new NullPointerException("Abbreviature can't be null");
			}
			else {
				for(LinkType item : values()) {
					if (item.getAbbreviature().equals(abbr)) {
						return item;
					}
				}
				throw new IllegalArgumentException("Abbreviature ["+abbr+"] not found in the enum");
			}
		}

		public static LinkType of(final int id) {
			for(LinkType item : values()) {
				if (item.getId() == id) {
					return item;
				}
			}
			throw new IllegalArgumentException("Id ["+id+"] not found in the enum");
		}
	}
	
	public static class Link {
		private final int		id;
		private final LinkType	type;
		private final int		left;
		private final int		right;
		
		public Link(final int id, final LinkType type, final int left, final int right) {
			this.id = id;
			this.type = type;
			this.left = left;
			this.right = right;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (id ^ (id >>> 32));
			result = prime * result + (int) (left ^ (left >>> 32));
			result = prime * result + (int) (right ^ (right >>> 32));
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Link other = (Link) obj;
			if (id != other.id) return false;
			if (left != other.left) return false;
			if (right != other.right) return false;
			if (type != other.type) return false;
			return true;
		}

		@Override
		public String toString() {
			return "Link [id=" + id + ", type=" + type + ", left=" + left + ", right=" + right + "]";
		}
	}
}
