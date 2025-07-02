package org.example.expert.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.example.expert.log.service.LogService;
import org.example.expert.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ManagerLogAspect {

    private final LogService logService;

    // @ManagerRegisterLog 어노테이션이 붙은 메서드 실행 후 호출되는 메서드
    @After("@annotation(org.example.expert.aop.annotation.ManagerRegisterLog)")
    public void logAfterManagerRegister(JoinPoint joinPoint) {
        try {
            // 현재 인증 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = "NULL";

            // 인증 정보가 있고 CustomUserDetails 타입이면 사용자 ID 추출
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                userId = String.valueOf(userDetails.getId());
            }

            // 현재 HTTP 요청 객체 가져오기
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();

            // 요청 URL 경로 가져오기
            String requestUrl = request.getRequestURI();

            // 호출된 메서드 이름 가져오기
            String methodName = joinPoint.getSignature().getName();

            // 호출된 메서드의 첫 번째 인자 값을 안전하게 문자열로 변환, 없으면 "no-args"
            String args = joinPoint.getArgs().length > 0 ?
                    getArgsSafely(joinPoint.getArgs()[0]) : "no-args";

            // 로그 메시지 문자열 생성
            String message = String.format("매니저 등록 요청 - UserID: %s, URL: %s, Method: %s, Args: %s",
                    userId, requestUrl, methodName, args);

            // 로그를 콘솔에 출력
            log.info("[Manager Register] {}", message);

            // 로그 내용을 DB에 저장
            logService.saveLog("MANAGER_REGISTER", message);

        } catch (Exception e) {
            // 로그 기록 중 예외 발생 시 에러 로그 출력, 서비스 흐름에는 영향 없음
            log.error("매니저 등록 로그 처리 중 오류 발생", e);
        }
    }

    // 첫 번째 인자 객체를 안전하게 문자열로 변환하는 보조 메서드
    private String getArgsSafely(Object arg) {
        // 인자가 null인 경우
        if (arg == null) {
            return "null";
        }

        // 클래스명이 ManagerSaveRequest를 포함하면 리플렉션으로 managerUserId만 추출하여 출력
        if (arg.getClass().getSimpleName().contains("ManagerSaveRequest")) {
            try {
                return "ManagerSaveRequest[managerId=" +
                        arg.getClass().getMethod("getManagerUserId").invoke(arg) + "]";
            } catch (Exception e) {
                // 실패 시 클래스 이름만 반환
                return arg.getClass().getSimpleName();
            }
        }

        // 그 외 일반 객체는 toString() 호출, 200자 이상이면 잘라서 반환
        String argStr = arg.toString();
        return argStr.length() > 200 ? argStr.substring(0, 200) + "..." : argStr;
    }
}
