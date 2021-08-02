package l2jorion.logger;

import java.io.Serializable;
import java.util.Iterator;

public interface Marker extends Serializable
{
	/**
	 * This constant represents any marker, including a null marker.
	 */
	public final String ANY_MARKER = "*";
	
	/**
	 * This constant represents any non-null marker.
	 */
	public final String ANY_NON_NULL_MARKER = "+";
	
	/**
	 * Get the name of this Marker.
	 * @return name of marker
	 */
	public String getName();
	
	/**
	 * Add a reference to another Marker.
	 * @param reference a reference to another marker
	 * @throws IllegalArgumentException if 'reference' is null
	 */
	public void add(Marker reference);
	
	/**
	 * Remove a marker reference.
	 * @param reference the marker reference to remove
	 * @return true if reference could be found and removed, false otherwise.
	 */
	public boolean remove(Marker reference);
	
	public boolean hasChildren();
	
	/**
	 * Does this marker have any references?
	 * @return true if this marker has one or more references, false otherwise.
	 */
	public boolean hasReferences();
	
	/**
	 * Returns an Iterator which can be used to iterate over the references of this marker. An empty iterator is returned when this marker has no references.
	 * @return Iterator over the references of this marker
	 */
	public Iterator<Marker> iterator();
	
	/**
	 * Does this marker contain a reference to the 'other' marker? Marker A is defined to contain marker B, if A == B or if B is referenced by A, or if B is referenced by any one of A's references (recursively).
	 * @param other The marker to test for inclusion.
	 * @throws IllegalArgumentException if 'other' is null
	 * @return Whether this marker contains the other marker.
	 */
	public boolean contains(Marker other);
	
	/**
	 * Does this marker contain the marker named 'name'? If 'name' is null the returned value is always false.
	 * @param name The marker name to test for inclusion.
	 * @return Whether this marker contains the other marker.
	 */
	public boolean contains(String name);
	
	/**
	 * Markers are considered equal if they have the same name.
	 * @param o
	 * @return true, if this.name equals o.name
	 * @since 1.5.1
	 */
	@Override
	public boolean equals(Object o);
	
	/**
	 * Compute the hash code based on the name of this marker. Note that markers are considered equal if they have the same name.
	 * @return the computed hashCode
	 * @since 1.5.1
	 */
	@Override
	public int hashCode();
	
}
