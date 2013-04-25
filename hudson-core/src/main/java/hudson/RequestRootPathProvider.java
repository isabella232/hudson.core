package hudson;

import javax.servlet.http.HttpServletRequest;

public interface RequestRootPathProvider {
	
	String getRootPath(HttpServletRequest req);

}
