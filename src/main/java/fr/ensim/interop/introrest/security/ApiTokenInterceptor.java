package fr.ensim.interop.introrest.security;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class ApiTokenInterceptor implements HandlerInterceptor {

    @Value("${api.token}")
    private String apiToken;

    @Override
    public boolean preHandle(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.equals("Bearer " + apiToken)) {
            return true; // autorisé
        }

        response.setStatus(javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("Unauthorized: invalid or missing API token.");
        return false;
    }
}
