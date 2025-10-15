package kopo.userservice.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import kopo.userservice.dto.TokenDTO;
import kopo.userservice.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@RefreshScope
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    // ✅ 고정 상수(코드 내 유지)
    public static final String HEADER_PREFIX = "Bearer ";

    // ✅ 외부 설정(Config Server)에서 주입
    @Value("${jwt.secret.key}")            private String secretKeyBase64;
    @Value("${jwt.token.creator}")         private String creator;
    @Value("${jwt.token.access.valid.time}")   private long accessValidSec;
    @Value("${jwt.token.access.name}")         private String accessTokenName;
    @Value("${jwt.token.refresh.valid.time}")  private long refreshValidSec;
    @Value("${jwt.token.refresh.name}")        private String refreshTokenName;

    /** JWT 생성 */
    public String createToken(String userId, String userName, String role, String managerId, JwtTokenType tokenType) {

        long validSec = (tokenType == JwtTokenType.ACCESS_TOKEN) ? accessValidSec : refreshValidSec;

        Claims claims = Jwts.claims()
                .setIssuer(creator)
                .setSubject(userId);          // PK
        claims.put("userName", userName);   // 사용자 이름 추가
        claims.put("roles", role);           // MANAGER / PATIENT (roles로 수정)
        claims.put("managerId", managerId);  // managerId 클레임 추가

        Date now = new Date();
        SecretKey secret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyBase64));

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + validSec * 1000))
                .signWith(secret, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 토큰에서 userId/role/userName 추출 */
    public TokenDTO getTokenInfo(String token) {
        SecretKey secret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyBase64));
        Claims claims = Jwts.parserBuilder().setSigningKey(secret).build()
                .parseClaimsJws(token).getBody();

        return TokenDTO.builder()
                .userId(CmmUtil.nvl(claims.getSubject()))
                .userName(CmmUtil.nvl((String) claims.get("userName"))) // 사용자 이름 추출
                .role(CmmUtil.nvl((String) claims.get("roles"))) // "role" → "roles"로 수정
                .build();
    }

    /** 요청에서 토큰 추출(쿠키 우선 → Authorization) */
    public String resolveToken(HttpServletRequest request, JwtTokenType type) {
        String tokenName = (type == JwtTokenType.ACCESS_TOKEN) ? accessTokenName : refreshTokenName;

        // 1) 쿠키
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (tokenName.equals(c.getName())) {
                    String v = CmmUtil.nvl(c.getValue());
                    if (!v.isEmpty()) return v;
                }
            }
        }
        // 2) Authorization: Bearer
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearer) && bearer.startsWith(HEADER_PREFIX)) {
            return bearer.substring(HEADER_PREFIX.length());
        }
        return "";
    }

    /** JWT에서 sub claim(userId) 추출 */
    public String getUserIdFromToken(String token) {
        System.out.println("[DEBUG] secretKeyBase64: " + secretKeyBase64);
        SecretKey secret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyBase64));
        System.out.println("[DEBUG] SecretKey: " + secret);
        Claims claims = Jwts.parserBuilder().setSigningKey(secret).build()
            .parseClaimsJws(token).getBody();
        System.out.println("[DEBUG] Claims: " + claims);
        return claims.getSubject();
    }
}
