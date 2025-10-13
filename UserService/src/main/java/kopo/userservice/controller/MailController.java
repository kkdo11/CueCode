package kopo.userservice.controller;

import kopo.userservice.dto.ContactFormDTO;
import kopo.userservice.dto.MailDTO;
import kopo.userservice.service.IMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class MailController {

    private final IMailService mailService;
    @PostMapping("/contact")

    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> contactUs(@RequestBody ContactFormDTO contactForm) {
        log.info(this.getClass().getName() + ".contactUs start!");

        try {
            MailDTO mailDTO = new MailDTO();
            mailDTO.setToMail("semin1125@gmail.com");
            mailDTO.setTitle("[CueCode 홈페이지 문의] " + contactForm.getName() + "님의 문의입니다.");

            String contents = "<html><body>" +
                              "<h3>새로운 문의가 도착했습니다.</h3>" +
                              "<p><strong>이름:</strong> " + contactForm.getName() + "</p>" +
                              "<p><strong>이메일:</strong> " + contactForm.getEmail() + "</p>" +
                              "<hr>" +
                              "<p><strong>내용:</strong></p>" +
                              "<p>" + contactForm.getMessage().replaceAll("\n", "<br>") + "</p>" +
                              "</body></html>";
            mailDTO.setContents(contents);
            int res = mailService.doSendMail(mailDTO);

            if (res == 1) {
                log.info("메일 발송 성공");
                return ResponseEntity.ok("메일 발송에 성공했습니다!");
            } else {
                log.warn("메일 발송 실패");
                return ResponseEntity.badRequest().body("메일 발송에 실패했습니다.");
            }

        } catch (Exception e) {
            log.error("메일 발송 처리 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body("서버 오류로 메일 발송에 실패했습니다.");
        } finally {
            log.info(this.getClass().getName() + ".contactUs end!");
        }
    }
}
