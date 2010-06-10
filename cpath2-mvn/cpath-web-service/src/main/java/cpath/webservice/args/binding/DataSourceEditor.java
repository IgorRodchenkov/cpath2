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

package cpath.webservice.args.binding;

import java.beans.PropertyEditorSupport;
import java.net.URI;

import org.bridgedb.DataSource;


/**
 * Converts a string parameter to org.bridgedb.DataSource
 * 
 * @author rodche
 *
 */
public class DataSourceEditor extends PropertyEditorSupport {
	
	/* (non-Javadoc)
	 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
	 */
	@Override
	public void setAsText(String ds) throws IllegalArgumentException {
		// guess it's a key (data source code)
		DataSource dataSource = DataSource.getBySystemCode(ds);
		
		if(dataSource == null) { // is full name?
			dataSource = DataSource.getByFullName(ds);
		}	
		
		// TODO the following might not required...
		if(dataSource == null) { // is URN?
			for(DataSource d : DataSource.getDataSources()) {
				URI u1 = URI.create(ds);
				URI u2 = URI.create(d.getURN(""));
				if(u1.equals(u2)) {
					dataSource = d;
					break;
				}
			}
		}
		
		setValue(dataSource);
	}
	
}
