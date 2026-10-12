package vn.macstudio.macstudio.web;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import vn.macstudio.macstudio.model.AppUser;
import vn.macstudio.macstudio.service.OAuthAccountService;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final OAuthAccountService accounts;
    private final SecurityContextRepository securityContexts = new HttpSessionSecurityContextRepository();

    public OAuthLoginSuccessHandler(OAuthAccountService accounts) {
        this.accounts = accounts;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)
            || !(oauthToken.getPrincipal() instanceof OidcUser oidcUser)) {
            throw new ServletException("Nhà cung cấp không trả về hồ sơ OpenID Connect hợp lệ.");
        }
        Map<String, Object> claims = oidcUser.getClaims();
        AppUser user;
        try {
            user = accounts.findOrCreateCustomer(
                oauthToken.getAuthorizedClientRegistrationId().toLowerCase(Locale.ROOT),
                oidcUser.getSubject(),
                oidcUser.getEmail(),
                displayName(claims),
                isEmailVerified(claims.get("email_verified")));
        } catch (AccessDeniedException exception) {
            response.sendRedirect(request.getContextPath() + "/account/login?oauthError");
            return;
        }

        var localAuthentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            user, null, AuthorityUtils.createAuthorityList(
                user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()));
        localAuthentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(localAuthentication);
        SecurityContextHolder.setContext(context);
        securityContexts.saveContext(context, request, response);
        response.sendRedirect(request.getContextPath() + "/");
    }

    private String displayName(Map<String, Object> claims) {
        Object name = claims.get("name");
        if (name instanceof String fullName && !fullName.isBlank()) return fullName;
        String givenName = claims.get("given_name") instanceof String value ? value : "";
        String familyName = claims.get("family_name") instanceof String value ? value : "";
        String joinedName = (givenName + " " + familyName).strip();
        return joinedName.isBlank() ? null : joinedName;
    }

    private boolean isEmailVerified(Object value) {
        return Boolean.TRUE.equals(value) || value instanceof String text && "true".equalsIgnoreCase(text);
    }
}
