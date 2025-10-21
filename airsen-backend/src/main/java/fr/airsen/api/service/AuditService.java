package fr.airsen.api.service;

import fr.airsen.api.entity.AdminActionLog;
import fr.airsen.api.entity.User;
import fr.airsen.api.repository.AdminActionLogRepository;
import fr.airsen.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for logging administrative actions in AIRSEN.
 */
@Service
@Transactional
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    @Autowired
    private AdminActionLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Logs an administrative action to the audit trail.
     *
     * This method creates a permanent record of admin actions for compliance,
     * security monitoring, and forensic investigations. The log entry includes
     * who performed the action, what was done, when it occurred, and from where.
     *
     * @param adminUserId ID of the admin user performing the action (nullable if admin deleted)
     * @param actionType Type of action performed (from AdminActionType enum)
     * @param targetResourceId ID of the affected resource (user, thread, message, etc.)
     * @param actionDetails Human-readable description of the action
     * @param ipAddress IP address of the admin performing the action (nullable)
     * @param userAgent User agent string from HTTP request (nullable)
     */
    public void logAdminAction(Long adminUserId, fr.airsen.api.entity.enums.AdminActionType actionType, 
                              Long targetResourceId, String actionDetails, String ipAddress, String userAgent) {
        try {
            // Fetch admin user entity (null if admin was deleted)
            User adminUser = null;
            if (adminUserId != null) {
                adminUser = userRepository.findById(adminUserId).orElse(null);
                if (adminUser == null) {
                    logger.warn("Admin user ID {} not found for audit log. Recording action without user reference.", adminUserId);
                }
            }

            // Create audit log entry
            AdminActionLog log = new AdminActionLog(
                adminUser,
                actionType.name(),
                targetResourceId,
                actionDetails,
                ipAddress,
                userAgent
            );

            // Persist to database
            auditLogRepository.save(log);

            logger.info("Admin action logged: {} by user {} on resource {}", 
                       actionType, adminUserId, targetResourceId);

        } catch (Exception e) {
            // Log error but don't throw - audit failure shouldn't block business operations
            logger.error("Failed to log admin action: {} by user {} on resource {}. Error: {}", 
                        actionType, adminUserId, targetResourceId, e.getMessage(), e);
        }
    }
}
