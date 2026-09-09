package com.heilous.global.auth;

import com.heilous.common.exception.CustomException;
import com.heilous.common.exception.GlobalErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String bearerToken = request.getHeader("Authorization");

            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {

                String token = bearerToken.substring(7);

                if (jwtProvider.validateToken(token)) {

                    String email = jwtProvider.getEmail(token);
                    String role = jwtProvider.getRole(token);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            );

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }

            filterChain.doFilter(request, response);

        } catch (CustomException e) {
            SecurityContextHolder.clearContext();
            writeErrorResponse(response, e.getErrorCode());
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            writeErrorResponse(response, GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void writeErrorResponse(HttpServletResponse response, GlobalErrorCode errorCode) throws IOException {
        if (response.isCommitted()) return;
        response.setStatus(errorCode.getStatus());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                String.format(
                        "{\"status\":%d,\"data\":{\"code\":\"%s\",\"message\":\"%s\"}}",
                        errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage()
                )
        );
    }
}