package cpath.webservice.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import cpath.config.CPathSettings;

/**
 * @author rodche
 *
 */
public final class CPathMaintenanceHandlerInterceptor extends HandlerInterceptorAdapter
{

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
			Object handler) throws Exception {
		
		String requestUri = request.getRequestURI();
		
		//disable user web service queries in admin mode is enabled
		if(CPathSettings.getInstance().isAdminEnabled() 				
			&& !(  requestUri.contains("/resources") 
				|| requestUri.contains("/help")
				|| requestUri.contains("/admin")
				|| requestUri.contains("/error")
				|| requestUri.contains("/login")
				|| requestUri.contains("/denied")
				|| requestUri.contains("/home")
				|| requestUri.contains("/datasources")
				|| requestUri.contains("/metadata/")
				|| requestUri.contains("/robots")
				|| requestUri.contains("/favicon")
				|| requestUri.contains("/logback")
				|| requestUri.contains("/log")
				|| requestUri.contains("/formats")
				|| requestUri.contains("/downloads")
				)
		)
		{
			response.sendError(503, CPathSettings
				.getInstance().property(CPathSettings.PROVIDER_NAME)
				+ " service maintenance.");
			return false;
		}
		else
			return true; 
	}

}
