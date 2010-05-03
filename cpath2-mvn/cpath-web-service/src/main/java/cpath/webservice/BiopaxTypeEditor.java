/**
 ** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/

package cpath.webservice;

import java.beans.PropertyEditorSupport;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;


/**
 * Helps convert request URL path values to a BioPAX type.
 * 
 * @author rodche
 *
 */
public class BiopaxTypeEditor extends PropertyEditorSupport {
	private static final Map<String, Class<? extends BioPAXElement>> typesByName;
	private static BioPAXFactory bioPAXFactory;
	
	static {
		typesByName = new TreeMap<String, Class<? extends BioPAXElement>>(); // want it sorted
		for (Method method : BioPAXLevel.L3.getDefaultFactory().getClass().getMethods())
		{
			if (method.getName().startsWith("create"))
			{
				Class<? extends BioPAXElement> clazz =
					(Class<? extends BioPAXElement>) method.getReturnType();
				
				typesByName.put(clazz.getSimpleName(), clazz);
			}
		}
		
		bioPAXFactory = BioPAXLevel.L3.getDefaultFactory();
	}
	
	
	/* (non-Javadoc)
	 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
	 */
	@Override
	public void setAsText(String type) {
		setValue(getSearchableBiopaxClassByName(type));
	}
	
	
	/*
	 * Enables using arguments, BioPAX class names, in any case: 
	 *     ProteinReference, PROTEINREFERENCE, proteinreference, etc.
	 * 
	 */
	private static Class<? extends BioPAXElement> getSearchableBiopaxClassByName(String type) {
		for(String key : typesByName.keySet()) {
			if(key.equalsIgnoreCase(type)) {
				/*
				Class<? extends BioPAXElement> persistentClass 
					= bioPAXFactory.reflectivelyCreate(typesByName.get(key)).getClass();
				return persistentClass;
				*/
				return typesByName.get(key);
			}
		}
		return null;
	}
	
	
	public static Map<String, Class<? extends BioPAXElement>> getTypesByName() {
		return typesByName;
	}
	
}
