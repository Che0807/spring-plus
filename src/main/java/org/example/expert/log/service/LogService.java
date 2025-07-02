package org.example.expert.log.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.log.entity.Log;
import org.example.expert.log.repository.LogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogService {
    private final LogRepository logRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(String action, String message) {
        try {
            Log newLog = new Log(action, message);
            logRepository.save(newLog);
            log.info("로그 저장 완료 - Action: {}", action);
        } catch (Exception e) {
            log.error("로그 저장 실패 - Action: {}, Message: {}", action, message, e);
        }
    }
}
