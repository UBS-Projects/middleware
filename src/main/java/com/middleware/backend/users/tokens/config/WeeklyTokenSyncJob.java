package com.middleware.backend.users.tokens.config;

import com.middleware.backend.users.model.User;
import com.middleware.backend.users.repository.UserRepository;
import com.middleware.backend.users.tokens.model.Token;
import com.middleware.backend.users.tokens.repository.TokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WeeklyTokenSyncJob {

    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public WeeklyTokenSyncJob(TokenRepository tokenRepository,
                              UserRepository userRepository,
                              EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    // Every Monday 09:00 Asia/Hebron
    @Scheduled(cron = "0 0 9 ? * MON", zone = "Asia/Hebron")
    public void run() {
        // Time windows
        ZoneId zone = ZoneId.of("Asia/Hebron");
        LocalDate today = LocalDate.now(zone);

        Instant now = ZonedDateTime.of(today, LocalTime.of(9,0), zone).toInstant();
        Instant from = now.plus(Duration.ofDays(1));   // +1 day
        Instant to   = now.plus(Duration.ofDays(10));  // +10 days

        Timestamp tsNow  = Timestamp.from(now);
        Timestamp tsFrom = Timestamp.from(from);
        Timestamp tsTo   = Timestamp.from(to);

        // Fetch tokens
        List<Token> expired = tokenRepository.findExpiredSystemUserTokens(tsNow);
        List<Token> expiring = tokenRepository.findSystemUserTokensExpiringBetween(tsFrom, tsTo);

        // Sort for readability
        expired.sort(Comparator.comparing(Token::getExpiresAt));
        expiring.sort(Comparator.comparing(Token::getExpiresAt));

        // Build HTML email
        String subject = "Weekly Token Sync Report (Expired & Expiring 1–10 days)";
        String html = buildEmailHtml(expired, expiring, zone);

        // Send to all ADMINs
        List<User> admins = userRepository.findAllAdmins();
        for (User admin : admins) {
            if (admin.getEmail() != null && !admin.getEmail().isBlank()) {
                emailService.sendHtml(admin.getEmail(), subject, html);
            }
        }
    }

    private String buildEmailHtml(List<Token> expired, List<Token> expiring, ZoneId zone) {
        String style = """
        <style>
          body{font-family:Arial,sans-serif;color:#222;}
          h2{color:#5371FF;margin-bottom:8px}
          table{border-collapse:collapse;width:100%;margin-bottom:18px}
          th,td{border:1px solid #e5e7eb;padding:8px;font-size:13px}
          th{background:#f3f4f6;text-align:left}
          .muted{color:#6b7280}
          .tag{display:inline-block;background:#e8eeff;color:#2940ff;padding:2px 6px;border-radius:6px;font-size:11px}
          .chips{margin:8px 0}
          .code{font-family:Consolas,monospace;font-size:12px}
        </style>
        """;

        String expiredRows;
        if (expired.isEmpty()) {
            expiredRows = """
            <tr><td colspan="4" class="muted">No expired tokens.</td></tr>
            """;
        } else {
            expiredRows = expired.stream()
                    .map(t -> String.format(
                            "<tr><td>%s</td><td class='code'>%s</td><td>%s</td><td>%s</td></tr>",
                            safeUser(t), mask(t.getToken()), t.getCreatedAt(), t.getExpiresAt()))
                    .collect(Collectors.joining());
        }

        String expiringRows;
        if (expiring.isEmpty()) {
            expiringRows = """
            <tr><td colspan="5" class="muted">No tokens expiring in the next 1–10 days.</td></tr>
            """;
        } else {
            expiringRows = expiring.stream()
                    .map(t -> {
                        long daysLeft = Duration.between(Instant.now(), t.getExpiresAt().toInstant()).toDays();
                        return String.format(
                                "<tr><td>%s</td><td class='code'>%s</td><td>%s</td><td>%s</td><td><span class='tag'>%d days</span></td></tr>",
                                safeUser(t), mask(t.getToken()), t.getCreatedAt(), t.getExpiresAt(), daysLeft);
                    })
                    .collect(Collectors.joining());
        }

        return """
        <html>
        <head>%s</head>
        <body>
          <h2>Weekly Token Sync</h2>
          <div class="chips">
            <span class="muted">Timezone: %s</span>
          </div>

          <h3>Expired Tokens</h3>
          <table>
            <thead>
              <tr><th>User</th><th>Token</th><th>Created At</th><th>Expired At</th></tr>
            </thead>
            <tbody>%s</tbody>
          </table>

          <h3>Tokens Expiring in 1–10 Days</h3>
          <table>
            <thead>
              <tr><th>User</th><th>Token</th><th>Created At</th><th>Expires At</th><th>Time Left</th></tr>
            </thead>
            <tbody>%s</tbody>
          </table>

          <p class="muted">This is an automated message from the Middleware system.</p>
        </body>
        </html>
        """.formatted(style, zone, expiredRows, expiringRows);
    }


    private String safeUser(Token t) {
        return t.getUser() == null ? "(no user)" :
                (t.getUser().getEmail() != null ? t.getUser().getEmail()
                        : String.valueOf(t.getUser().getId()));
    }

    // Mask token for email (recommended). If you truly must send full tokens,
    // replace this with: return token == null ? "" : token;
    private String mask(String token) {
        if (token == null || token.length() <= 8) return token == null ? "" : token;
        return token.substring(0, 4) + "..." + token.substring(token.length()-4);
    }
}