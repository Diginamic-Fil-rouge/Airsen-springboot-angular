package fr.airsen.api.service;

import fr.airsen.api.entity.AdminActionLog;
import fr.airsen.api.entity.User;
import fr.airsen.api.repository.AdminActionLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@Transactional
public class AuditService {

    @Autowired
    private AdminActionLogRepository auditLogRepository;

    public void logAdminAction(Long adminUserId, fr.airsen.api.entity.enums.AdminActionType actionType, Long targetResourceId, String actionDetails, String ipAddress, String userAgent) {
        // This is a placeholder implementation.
        // You'll need to fetch the admin User entity from the database.
        // For now, we'll just create a new log entry.
        AdminActionLog log = new AdminActionLog();
        // User adminUser = userRepository.findById(adminUserId).orElse(null);
        // log.setAdminUser(adminUser);
        log.setActionType(actionType.name());
        log.setTargetResourceId(targetResourceId);
        log.setActionDetails(actionDetails);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        auditLogRepository.save(log);
    }
}
