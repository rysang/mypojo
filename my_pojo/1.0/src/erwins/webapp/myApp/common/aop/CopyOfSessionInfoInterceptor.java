package erwins.webapp.myApp.common.aop;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.google.appengine.api.users.UserService;

import erwins.util.lib.CollectionUtil;
import erwins.util.web.WebUtil;
import erwins.webapp.myApp.ChannelComponent;
import erwins.webapp.myApp.Config;
import erwins.webapp.myApp.Current;
import erwins.webapp.myApp.Menu;
import erwins.webapp.myApp.SystemInfo;
import erwins.webapp.myApp.user.GoogleUser;
import erwins.webapp.myApp.user.GoogleUserService;
import erwins.webapp.myApp.user.SessionInfo;

public class CopyOfSessionInfoInterceptor implements HandlerInterceptor {

	private Log log = LogFactory.getLog(this.getClass());
	@Autowired private GoogleUserService googleUserService;
	@Autowired private UserService userService;
	@Autowired private ChannelComponent channelHelper;

	/** 3. 뷰까지 생성 이후에 적용됨. 요청 처리중에 생성한 리소스 반환에 적합. */
	@Override
	public void afterCompletion(HttpServletRequest req, HttpServletResponse arg1, Object arg2, Exception arg3)
			throws Exception {
		Current.clear();
		log.debug("ThreadLocal 반환");
	}
	/** 2. 작업 결과 조작 가능. */
	@Override
	public void postHandle(HttpServletRequest req, HttpServletResponse arg1, Object arg2, ModelAndView arg3)
			throws Exception {}

	/** 1. 요청정보 가공용. false이면 이후 컨트롤러 등의 작동이 중단된다. 
	 * 권한 기본을 넣어주고, 관리자이면 추가 권한을 부여한다.*/
	@Override
	public boolean preHandle(HttpServletRequest req, HttpServletResponse arg1, Object arg2) throws Exception {
		SessionInfo info = Current.getInfo();
		String requestedUrl = WebUtil.getUrl(req);
		info.setUrl(requestedUrl);
		info.setLogin(userService.isUserLoggedIn());
		if (userService.isUserLoggedIn()) {
			String mailAddress = userService.getCurrentUser().getEmail();
			if( !SystemInfo.isServer() && mailAddress.equals("qq")) mailAddress = "my.pojo@gmail.com"; //테스트용 간단로그인 
			GoogleUser user = googleUserService.getByGoogleMail(mailAddress);
			//최초 관리자가 로그인할 경우 user가 null이다. 그때를 위해 널체크를 해준다.
			if(user!=null && CollectionUtil.isEqualsAny(Config.ADMIN_ID, mailAddress)) user.addRoles(GoogleUser.ROLE_ADMIN);
			info.setUser(user);
			initToken(req, user);
		}
		info.setMenu(Menu.getMenuByStartWith(requestedUrl));
		
		//MXPP받을때 접두 URL이 이걸로 시작한다.. 제길.
		if(requestedUrl.startsWith("/_ah/")) info.setMenu(Menu.index);
		log.debug("ThreadLocal 초기화");
		return true;
	}

	/** 푸시서비스용 채널토큰 준비. */
	private void initToken(HttpServletRequest req, GoogleUser user) {
		if(user==null) return;
		String token = channelHelper.getOrCreateToken(user.getId());
		req.setAttribute("token", token);
	}

}