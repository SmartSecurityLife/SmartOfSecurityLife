/**
 * 
 */
package org.frameworkset.web.session;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.frameworkset.security.session.Session;
import org.frameworkset.security.session.SessionUtil;
import org.frameworkset.security.session.impl.HttpSessionImpl;
import org.frameworkset.security.session.impl.SessionHttpServletRequestWrapper;
import org.frameworkset.security.session.impl.SessionID;
import org.frameworkset.web.auth.AuthenticateException;
import org.frameworkset.web.auth.AuthenticateMessages;
import org.frameworkset.web.auth.AuthenticatedToken;
import org.frameworkset.web.auth.AuthorHelper;
import org.frameworkset.web.auth.TicketConsts;

import com.frameworkset.util.StringUtil;

/**
 * @author yinbp
 *
 * @Date:2016-11-20 22:24:58
 */
public class TicketSessionHttpServletRequestWrapper extends SessionHttpServletRequestWrapper {
	protected AuthenticatedToken token;
	protected boolean gensessionfromauthsessionid;
	protected boolean tokenfromlocalcookie;
	protected String authenticateCode;
	/**
	 * @param request
	 * @param response
	 * @param servletContext
	 */
	public TicketSessionHttpServletRequestWrapper(HttpServletRequest request, HttpServletResponse response,
			ServletContext servletContext) {
		super(request, response, servletContext);
		if( !usewebsession)
		{
			 authenticateCode = request.getParameter(TicketConsts.ticket_authenticatecode_parameter_name);//首先从请求参数中获取authenticateCode
			if(authenticateCode == null)
			{
				authenticateCode = request.getHeader(TicketConsts.ticket_authenticatecode_parameter_name);//如果从请求参数中获取authenticateCode为空，则从请求头中获取
			}
			if(authenticateCode == null)
			{
				authenticateCode = StringUtil.getCookieValue(request,TicketConsts.ticket_authenticatecode_parameter_name);//如果从请求头中获取authenticateCode为空，则从cookie中获取
			}
			
			if(authenticateCode != null)
			{
				try {
					token = AuthorHelper.decodeMessageResponse(authenticateCode);
					String token_sessionid = token.getSessionid();
					if(token_sessionid != null)
					{
						if(sessionid == null)
						{
							this.sessionid = new SessionID();
							sessionid.setSessionId( token_sessionid);
							sessionid.setSignSessionId(SessionUtil.getSessionManager().getSignSessionIDGenerator().sign(token_sessionid,false));
							gensessionfromauthsessionid = true;
						}
						else if(!sessionid.getSessionId().equals(token_sessionid))
						{
							this.sessionid = new SessionID();
							sessionid.setSessionId( token_sessionid);
							sessionid.setSignSessionId(SessionUtil.getSessionManager().getSignSessionIDGenerator().sign(token_sessionid,false));
							gensessionfromauthsessionid = true;
						}
					}
				} catch (AuthenticateException e) {
					
					throw new AuthenticateException(AuthenticateMessages.getMessage(e.getMessage()),e);
				}
			}
			else if(this.sessionid != null)
			{
				authenticateCode = StringUtil.getCookieValue((HttpServletRequest)request, sessionid.getSessionId());
				if(authenticateCode != null)
				{
					try {
						token = AuthorHelper.decodeMessageResponse(authenticateCode);						
						tokenfromlocalcookie = true;  
					} catch (AuthenticateException e) {
						
						throw new AuthenticateException(AuthenticateMessages.getMessage(e.getMessage()),e);
					}
				}
			}
		}
	}
	
	
	@Override
	public HttpSessionImpl buildHttpSessionImpl(Session session)
	{
		if(this.gensessionfromauthsessionid)
		{
			SessionUtil.writeCookies_(this, response, sessionid.getSessionId(),authenticateCode);
			if(!sessionidcookiewrited )
			{
				SessionUtil.writeSessionIDCookies(this, response,SessionUtil.getSessionManager().getCookiename(),sessionid);
				sessionidcookiewrited = true; 
			}
		}
		return new TicketHttpSessionImpl(token,authenticateCode,session,servletContext,this.getContextPath(),this);
	}
	
	@Override
	public void invalidateCallback() {
		token = null;
		gensessionfromauthsessionid = false;
		tokenfromlocalcookie = false;
		authenticateCode = null;
		
		super.invalidateCallback();
		
	}

}
