package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.limewire.collection.IntWrapper;
import org.limewire.util.Base32;

/**
 * This class represents an individual Uniform Resource Name (URN), as
 * specified in RFC 2141.  This does extensive validation of URNs to 
 * make sure that they are valid, with the factory methods throwing 
 * exceptions when the arguments do not meet URN syntax.  This does
 * not perform rigorous verification of the SHA1 values themselves.
 *
 * This class is immutable.
 *
 * @see UrnCache
 * @see FileDesc
 * @see java.io.Serializable
 */
public final class URN implements Serializable {
    
    /** The range of all types for URNs. */
    public static enum Type {        
        /** UrnType for a SHA1 URN */
        SHA1("sha1:",32),
        
        /** UrnType for a bitprint URN */
        BITPRINT("bitprint:",72),
        
        /** UrnType for a Tiger Tree root URN */
        TTROOT("ttroot:",39),
        
        /** UrnType for any kind of URN. */
        ANY_TYPE("",-1),

        /** UrnType for an invalid Urn Type. */
        INVALID("Invalid",-1),
        
        /** UrnType for a {@link GUID} URN. */
        GUID("guid:", 32);
        
        private final String descriptor;
        
        private final int length;
        
        private Type(String descriptor, int length) {
            this.descriptor = descriptor;
            this.length = length;
        }
        
        /** Returns the descriptor used for printing out a URN of this type. */
        public String getDescriptor() {
            return descriptor;
        }
        
        public int getLength() {
            return length;
        }

        /** A set of types that allow only SHA1s. */
        public static final Set<URN.Type> SHA1_SET = EnumSet.of(SHA1);     

        /** A set of types that allow any kind of URN. */
        public static final Set<URN.Type> ANY_TYPE_SET = EnumSet.of(ANY_TYPE);
        
        /** A set of types that disallow any URN. */
        public static final Set<URN.Type> NO_TYPE_SET = EnumSet.noneOf(URN.Type.class);
        
        /**
         * The leading URN string identifier, as specified in
         * RFC 2141.  This is equal to "urn:", although note that this
         * should be used in a case-insensitive manner in compliance with
         * the URN specification (RFC 2141).
         */
        public static final String URN_NAMESPACE_ID = "urn:";

        /**
         * Returns the string representation of this URN type.
         *
         * @return the string representation of this URN type
         */
        public String toString() {
            return URN_NAMESPACE_ID + descriptor;
        }
        
        static URN.Type createFromDescriptor(String desc) {
            desc = desc.toLowerCase(Locale.US).trim();
            for(Type type : values())
                if(type.descriptor.equals(desc))
                    return type;
            return null;
        }

        /**
         * Factory method for obtaining <tt>UrnType</tt> instances from strings.
         * If the isSupportedUrnType method returns <tt>true</tt> this is
         * guaranteed to return a non-null UrnType.
         *
         * @param type the string representation of the urn type
         * @return the <tt>UrnType</tt> instance corresponding with the specified
         *  string, or <tt>null</tt> if the type is not supported
         */
        public static URN.Type createUrnType(String value) {
            value = value.toLowerCase(Locale.US).trim();
            for(Type type : values())
                if(type.toString().equals(value))
                    return type;
            return null;
        }

        /**
         * Returns whether or not the string argument is a urn type that
         * we know about.
         *
         * @param value to string to check 
         * @return <tt>true</tt> if it is a valid URN type, <tt>false</tt>
         *  otherwise
         */
        public static boolean isSupportedUrnType(String value) {
            return createUrnType(value) != null;
        }
    }

	private static final long serialVersionUID = -6053855548211564799L;
    
    /** An empty set casted to URNs. */
    public static final Set<URN> NO_URN_SET = Collections.emptySet();
	
	/**
	 * A constant invalid URN that classes can use to represent an invalid URN.
	 */
	public static final URN INVALID = new URN("bad:bad", Type.INVALID);
	
	/**
	 * The amount of time we must be idle before we start
	 * devoting all processing time to hashing.
	 * (Currently 5 minutes).
	 */
	public static final int MIN_IDLE_TIME = 5 * 60 * 1000;

	/**
     * Cached constant to avoid making unnecessary string allocations
     * in validating input.
     */
    private static final String DOT = ".";

    /**
	 * The string representation of the URN.
	 */
	private transient String _urnString;

	/**
	 * Variable for the <tt>UrnType</tt> instance for this URN.
	 */
	private transient Type _urnType;

	/**
	 * Cached hash code that is lazily initialized.
	 */
	private volatile transient int hashCode = 0;
	
	/**
	 * The progress of files currently being hashed.
	 * Files are added to this when hashing is started
	 * and removed when hashing finishes.
	 * IntWrapper stores the amount of bytes read.
	 */
	private static final Map<File, IntWrapper> progressMap =
	    Collections.synchronizedMap(new HashMap<File, IntWrapper>());
	
	/**
	 * Gets the amount of bytes hashed for a file that is being hashed.
	 * Returns -1 if the file is not being hashed at all.
	 */
	public static int getHashingProgress(File file) {
	    IntWrapper progress = progressMap.get(file);
	    if ( progress == null )
	        return -1;
	    else
	        return progress.getInt();
	}
    
	/**
	 * Creates a new <tt>URN</tt> instance from the specified string.
	 * The resulting URN can have any Namespace Identifier and any
	 * Namespace Specific String.
	 *
	 * @param urnString a string description of the URN.  Typically 
     *  this will be a SHA1 containing a 32-character value, e.g., 
     *  "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB".
	 * @return a new <tt>URN</tt> instance
	 * @throws <tt>IOException</tt> urnString was malformed or an
     *  unsupported type
	 */
	public static URN createSHA1Urn(final String urnString) 
		throws IOException {
        String typeString = URN.getTypeString(urnString).toLowerCase(Locale.US);
        if (typeString.indexOf(Type.SHA1.getDescriptor()) == 4)
    		return createUrnFromString(urnString);
        else if (typeString.indexOf(Type.BITPRINT.getDescriptor()) == 4)
            return createSHA1UrnFromBitprint(urnString);
        else
            throw new IOException("unsupported or malformed URN");
	}
	
	/**
	 * Creates a GUID URN from a string.
	 * 
	 * @param urnString string of the format "urn:guid:[hexstringoflength32]"
	 * @return a URN with urn type {@link Type#GUID}
	 * 
	 * @throws IOException if the string has an invalid URN format or is not of type {@link Type#GUID}
	 */
	public static URN createGUIDUrn(final String urnString) throws IOException {
	    URN urn = createUrnFromString(urnString);
	    if (urn.getUrnType() != Type.GUID) {
	        throw new IOException("Not a GUID urn: " + urnString);
	    }
	    return urn;
	}

	/**
	 * Creates a URN for a guid.
	 */
	public static URN createGUIDUrn(final GUID guid) {
	    return new URN(Type.URN_NAMESPACE_ID + Type.GUID.getDescriptor() + guid.toHexString(), Type.GUID);
	}

    /**
     * @param ttroot a base32 encoded root of a hash tree
     * @return an URN object for that root
     */
    public static URN createTTRootUrn(String urnString) 
    throws IOException {
        String typeString = URN.getTypeString(urnString).toLowerCase(Locale.US);
        if (typeString.indexOf(Type.TTROOT.getDescriptor()) == 4)
            return new URN(urnString, Type.TTROOT);
        else
            throw new IOException("unsupported or malformed URN");
    }
    
	/**
	 * Retrieves the TigerTree Root hash from a bitprint string.
	 */
	public static String getTigerTreeRoot(final String urnString) throws IOException {
        String typeString = URN.getTypeString(urnString).toLowerCase(Locale.US);
        if (typeString.indexOf(Type.BITPRINT.getDescriptor()) == 4)
            return getTTRootFromBitprint(urnString);
        else
            throw new IOException("unsupported or malformed URN");
    }
	    

	/**
	 * Convenience method for creating a SHA1 <tt>URN</tt> from a <tt>URL</tt>.
	 * For the url to work, its getFile method must return the SHA1 urn
	 * in the form:<p> 
	 * 
	 *  /uri-res/N2R?urn:sha1:SHA1URNHERE
	 * 
	 * @param url the <tt>URL</tt> to extract the <tt>URN</tt> from
	 * @throws <tt>IOException</tt> if there is an error reading the URN from
	 *  the URL
	 */
	public static URN createSHA1UrnFromURL(final URL url) 
		throws IOException {
		return createSHA1UrnFromUriRes(url.getFile());
	}

	/**
	 * Convenience method for creating a <tt>URN</tt> instance from a string
	 * in the form:<p>
	 *
	 * /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB
	 */
	public static URN createSHA1UrnFromUriRes(String sha1String) 
		throws IOException {
		sha1String.trim();
		if(isValidUriResSHA1Format(sha1String)) {
			return createUrnFromString(sha1String.substring(13));
		} else {
			throw new IOException("could not parse string format: "+sha1String);
		}
	}
    
    /**
     * Creates a SHA1 URN from a byte[].
     */
    public static URN createSHA1UrnFromBytes(byte[] bytes) throws IOException {
        if(bytes == null || bytes.length != 20)
            throw new IOException("invalid bytes!");
        
        String hash = Base32.encode(bytes);
        return createUrnFromString("urn:sha1:" + hash);
    }
    
    /**
     * Creates a TTROOT URN from a byte[].
     */
    public static URN createTTRootFromBytes(byte [] bytes) throws IOException {
        if(bytes == null || bytes.length != 24)
            throw new IOException("invalid bytes!");
        
        String hash = Base32.encode(bytes);
        return new URN(Type.URN_NAMESPACE_ID+Type.TTROOT.getDescriptor()+hash, Type.TTROOT);
    }

	/**
	 * Convenience method that runs a standard validation check on the URN
	 * string before calling the <tt>URN</tt> constructor.
	 *
	 * @param urnString the string for the urn
	 * @return a new <tt>URN</tt> built from the specified string
	 * @throws <tt>IOException</tt> if there is an error
	 */
	private static URN createUrnFromString(final String urnString) 
		throws IOException {
		if(!URN.isValidUrn(urnString)) {
			throw new IOException("invalid urn string: "+urnString);
		}
		String typeString = URN.getTypeString(urnString);
        Type type = Type.createUrnType(typeString);
        if(type == null)
			throw new IOException("urn type not recognized: "+typeString);
		return new URN(urnString, type);
	}

	/**
     * Constructs a new SHA1 URN from a bitprint URN
     * 
     * @param bitprintString
     *            the string for the bitprint
     * @return a new <tt>URN</tt> built from the specified string
     * @throws <tt>IOException</tt> if there is an error
     */
    private static URN createSHA1UrnFromBitprint(final String bitprintString)
        throws IOException {
        // extract the BASE32 encoded SHA1 from the bitprint
        int dotIdx = bitprintString.indexOf(DOT);
        if(dotIdx == -1)
            throw new IOException("invalid bitprint: " + bitprintString);

        String sha1 =
            bitprintString.substring(
                bitprintString.indexOf(':', 4) + 1, dotIdx);

        return createUrnFromString(
            Type.URN_NAMESPACE_ID + Type.SHA1.getDescriptor() + sha1);
    }
    
	/**
     * Gets the TTRoot from a bitprint string.
     */
    private static String getTTRootFromBitprint(final String bitprintString)
      throws IOException {
        int dotIdx = bitprintString.indexOf(DOT);
        if(dotIdx == -1 || dotIdx == bitprintString.length() - 1)
            throw new IOException("invalid bitprint: " + bitprintString);

        String tt = bitprintString.substring(dotIdx + 1);
        if(tt.length() != 39)
            throw new IOException("wrong length: " + tt.length());

        return tt;
    }
    
	/**
	 * Constructs a new URN based on the specified <tt>File</tt> instance.
	 * The constructor calculates the SHA1 value for the file, and is a
	 * costly operation as a result.
	 *
	 * @param file the <tt>File</tt> instance to construct the URN from
	 * @param urnType the type of URN to construct for the <tt>File</tt>
	 *  instance, such as SHA1_URN
	 */
	private URN(final String urnString, final Type urnType) {
        int lastColon = urnString.lastIndexOf(":");
        String nameSpace = urnString.substring(0,lastColon+1);
        String hash = urnString.substring(lastColon+1);
		this._urnString = nameSpace.toLowerCase(Locale.US) +
                                  hash.toUpperCase(Locale.US);
		this._urnType = urnType;
	}
    
    /**
     * Returns the bytes of the namespace specific string URN.
     * 
     * TODO: If the URN wasn't stored in Base32, this will be wrong.
     *       We deal only with SHA1 right now, which will be Base32.
     */
    public byte[] getBytes() {
        return Base32.decode(getNamespaceSpecificString());        
    }

    /**
     * Returns the namespace specific string part of the URN, this is
     * the part after the second colon.
     */
    public String getNamespaceSpecificString() {
        return _urnString.substring(_urnString.lastIndexOf(':') + 1); 
    }
    
	/**
	 * Returns the <tt>UrnType</tt> instance for this <tt>URN</tt>.
	 *
	 * @return the <tt>UrnType</tt> instance for this <tt>URN</tt>
	 */
	public Type getUrnType() {
		return _urnType;
	}

	// implements HTTPHeaderValue
	public String httpStringValue() {
		return _urnString;
	}

	/**
	 * Returns whether or not the URN_STRING argument is a valid URN 
	 * string, as specified in RFC 2141.
	 *
	 * @param urnString the urn string to check for validity
	 * @return <tt>true</tt> if the string argument is a URN, 
	 *  <tt>false</tt> otherwise
	 */
	public static boolean isUrn(final String urnString) {
		return URN.isValidUrn(urnString);
	}

	/**
	 * Returns whether or not this URN is a SHA1 URN.  Note that a bitprint
	 * URN will return false, even though it contains a SHA1 hash.
	 *
	 * @return <tt>true</tt> if this is a SHA1 URN, <tt>false</tt> otherwise
	 */
	public boolean isSHA1() {
		return _urnType == Type.SHA1;
	}
    
    /**
     * Returns whether or not this URN is a Tiger Tree Root URN.
     *
     * @return <tt>true</tt> if this is a Tiger Tree Root URN, <tt>false</tt> otherwise
     */
    public boolean isTTRoot() {
        return _urnType == Type.TTROOT;
    }
    
    /**
     * Returns whether or not this URN is a GUID URN.
     *
     * @return <tt>true</tt> if this is a GUID URN, <tt>false</tt> otherwise
     */
    public boolean isGUID() {
        return _urnType == Type.GUID;
    }

    /**
     * Checks for URN equality.  For URNs to be equal, their URN strings must
     * be equal.
     *
     * @param o the object to compare against
     * @return <tt>true</tt> if the URNs are equal, <tt>false</tt> otherwise
     */
    public boolean equals(Object o) {
        if(o == this) return true;
        if (!(o instanceof URN)) return false;

        // Since hashCode is cached, this speeds comparison 
        // without affecting accuracy.
        if (this.hashCode() != o.hashCode()) {
            return false;
        }
        
        URN urn = (URN)o;
		
        return (_urnString.equals(urn._urnString) &&
                    _urnType.equals(urn._urnType));
        }

	/**
	 * Overrides the hashCode method of Object to meet the contract of 
	 * hashCode.  Since we override equals, it is necessary to also 
	 * override hashcode to ensure that two "equal" instances of this
	 * class return the same hashCode, less we unleash unknown havoc on 
	 * the hash-based collections.
	 *
	 * @return a hash code value for this object
	 */
	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = (37*result) + this._urnString.hashCode();
			result = (37*result) + this._urnType.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	/**
	 * Overrides toString to return the URN string.
	 *
	 * @return the string representation of the URN
	 */
	public String toString() {
		return _urnString;
	}

	/**
	 * This.method checks whether or not the specified string fits the
	 * /uri-res/N2R?urn:sha1: format.  It does so by checking the start of the
	 * string as well as verifying the overall length.
	 *
	 * @param sha1String the string to check
	 * @return <tt>true</tt> if the string follows the proper format, otherwise
	 *  <tt>false</tt>
	 */
	private static boolean isValidUriResSHA1Format(final String sha1String) {
		String copy = sha1String.toLowerCase(Locale.US);		
		if(copy.startsWith("/uri-res/n2r?urn:sha1:")) {
			// just check the length
			return sha1String.length() == 54;
		} 
		return false;
	}

	/**
	 * Returns the URN type string for this URN.  This requires that each URN 
	 * have a specific type - a general "urn:" type is not accepted.  As an example
	 * of how this method behaves, if the string for this URN is:<p>
	 * 
	 * urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB <p>
	 *
	 * then this method will return: <p>
	 *
	 * urn:sha1:
	 *
	 * @param fullUrnString the string containing the full urn
	 * @return the urn type of the string
	 */
	private static String getTypeString(final String fullUrnString)
	  throws IOException {		
		// trims any leading whitespace from the urn string -- without 
		// whitespace the urn must start with 'urn:'
		String type = fullUrnString.trim();
		if(type.length() <= 4)
		    throw new IOException("no type string");

		return type.substring(0,type.indexOf(':', 4)+1); 
	}

	/**
	 * Returns whether or not the specified string represents a valid 
	 * URN.  For a full description of what qualifies as a valid URN, 
	 * see RFC2141 ( http://www.ietf.org ).<p>
	 *
	 * The broad requirements of the URN are that it meet the following 
	 * syntax: <p>
	 *
	 * <URN> ::= "urn:" <NID> ":" <NSS>  <p>
	 * 
	 * where phrases enclosed in quotes are required and where "<NID>" is the
	 * Namespace Identifier and "<NSS>" is the Namespace Specific String.
	 *
	 * @param urnString the <tt>String</tt> instance containing the http request
	 * @return <tt>true</tt> if the specified string represents a valid urn,
	 *         <tt>false</tt> otherwise
	 */
	private static boolean isValidUrn(final String urnString) {
		int colon1Index = urnString.indexOf(":");
		if(colon1Index == -1 || colon1Index+1 > urnString.length()) {
			return false;
		}

		int urnIndex1 = colon1Index-3;
		int urnIndex2 = colon1Index+1;

		if((urnIndex1 < 0) || (urnIndex2 < 0)) {
			return false;
		}

		// get the last colon -- this should separate the <NID>
		// from the <NIS>
		int colon2Index = urnString.indexOf(":", colon1Index+1);
		
		if(colon2Index == -1 || colon2Index+1 > urnString.length())
		    return false;
		
		String urnType = urnString.substring(0, colon2Index+1);
        Type type = Type.createUrnType(urnType);
        if (type == null)
            return false;
        if (type.getLength() != urnString.substring(colon2Index+1).length())
            return false;
		return true;
	}
    
	/**
	 * Serializes this instance.
	 *
	 * @serialData the string representation of the URN
	 */
	private void writeObject(ObjectOutputStream s) 
		throws IOException {
		s.defaultWriteObject();
		s.writeUTF(_urnString);
		s.writeObject(_urnType);
	}

	/**
	 * Deserializes this <tt>URN</tt> instance, validating the urn string
	 * to ensure that it's valid.
	 */
	@SuppressWarnings("deprecation")
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        _urnString = s.readUTF();
        Object type = s.readObject();
        // convert from older serialized UrnTypes to the URN.Type enum
        if(type instanceof UrnType)
            type = Type.createFromDescriptor(((UrnType)type).getType());        
        _urnType = (Type)type;
        
        if(_urnType != URN.Type.SHA1 && _urnType != URN.Type.TTROOT)
            throw new InvalidObjectException("invalid urn type: " + type);

        if (!URN.isValidUrn(_urnString))
            throw new InvalidObjectException("invalid urn: "+_urnString);		
	}
}
