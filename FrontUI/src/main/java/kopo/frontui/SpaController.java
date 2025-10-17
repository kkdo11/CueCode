package kopo.frontui;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletResponse response) {
        // 404 Not Found 오류가 발생하면 index.html로 포워딩합니다.
        if (response.getStatus() == HttpStatus.NOT_FOUND.value()) {
            return "forward:/index.html";
        }
        // 다른 종류의 오류는 기본 오류 페이지를 보여줍니다.
        return "error";
    }
}