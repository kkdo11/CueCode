package kopo.userservice.service.impl;

import jakarta.mail.internet.MimeMessage;
import kopo.userservice.dto.MailDTO;
import kopo.userservice.service.IMailService;
import kopo.userservice.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class MailService implements IMailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromMail;

    @Override
    public int doSendMail(MailDTO pDTO) {
        log.info(this.getClass().getName() + ".doSendMail start!");

        int res = 1;

        if (pDTO == null) {
            pDTO = new MailDTO();
        }

        String toMail = CmmUtil.nvl(pDTO.getToMail());
        String title = CmmUtil.nvl(pDTO.getTitle());
        String contents = CmmUtil.nvl(pDTO.getContents());

        log.info("toMail : " + toMail);
        log.info("title : " + title);
        log.info("contents : " + contents);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(message, "UTF-8");

        try {
            messageHelper.setTo(toMail);
            messageHelper.setFrom(fromMail);
            messageHelper.setSubject(title);
            messageHelper.setText(contents, true);

            mailSender.send(message);

        } catch (Exception e) {
            // 실제 실패 원인 메시지를 포함하여 로그 및 예외 생성
            String errorMessage = "이메일 발송 중 오류 발생: " + e.getMessage();
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }

        log.info(this.getClass().getName() + ".doSendMail end!");
        return res;
    }
}
