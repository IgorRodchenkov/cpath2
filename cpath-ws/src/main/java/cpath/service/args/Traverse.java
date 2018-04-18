package cpath.service.args;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Traverse extends ServiceQuery {
	@NotBlank(message="Property Path is blank (not specified).")
	private String path;
	// required at least one value
	@NotEmpty(message="Provide at least one URI.")
	private String[] uri;
	
	public Traverse() {
	}

	public String[] getUri() {
		return uri;
	}

	public void setUri(String[] uri) {
		Set<String> uris = new HashSet<>(uri.length);
		for(String item : uri) {
			if(item.contains(",")) {
				//split by ',' ignoring spaces and empty values (between ,,)
				for(String id : item.split("\\s*,\\s*", -1))
					uris.add(id);
			} else {
				uris.add(item);
			}
		}
		this.uri = uris.toArray(new String[uris.size()]);
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String toString() {
		return String.format("%s p:%s; uri:%s", super.toString(), path, Arrays.toString(uri));
	}

	@Override
	public String getCommand() {
		return "traverse";
	}

	@Override
	public String getFormatName() {
		return "xml"; //default
	}
}
