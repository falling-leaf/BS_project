package demo.controller;

// 基本实现Restful API的包
import demo.utils.APIResponse;
import demo.entity.User;
import demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// 正则表达式的包
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 发送邮件的包
import demo.utils.EmailSender;
import demo.utils.JwtUtil;

// 加密的包
import demo.utils.Encoder;


@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    // 1. 登录功能
    @PostMapping("/login")
    public ResponseEntity<APIResponse> login(@RequestParam String account, @RequestParam String password) {
        String res_password = userService.login(account);
        if (res_password == null)
            return ResponseEntity.ok(new APIResponse("用户不存在", 200));
        else {
            if (!res_password.equals(Encoder.sha256(password)))
                return ResponseEntity.ok(new APIResponse("密码错误", 200));
            else {
                 String jwt_value =  JwtUtil.createJWT("login", account, password);
                return ResponseEntity.ok(new APIResponse("login successfully", 200, jwt_value));
            }
        }
    }

    // 2. 注册功能
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestParam String account, @RequestParam String password, @RequestParam String email, @RequestParam String code, @RequestParam String jwt_value) {
        // 若存在账户则创建失败
        String res_account = userService.check_account(account);
        if (res_account != null)
            return ResponseEntity.ok("账户已存在");

        // 若存在邮箱则创建失败
        String res_email = userService.check_email(email);
        if (res_email != null)
            return ResponseEntity.ok("邮箱已被注册");

        // 检查账户/密码是否过短
        if (account.length() < 6)
            return ResponseEntity.ok("账户名过短");
        if (password.length() < 6)
            return ResponseEntity.ok("密码过短");
        // 检查邮箱格式
        if (!isValidEmail(email))
            return ResponseEntity.ok("邮箱格式有误");
        try {
            if (!JwtUtil.paraJWT2code(jwt_value).equals(Encoder.sha256(code))) {
                return ResponseEntity.ok("验证码错误");
            }
        } catch (Exception e) {
            return ResponseEntity.ok("验证码已失效，请重新发送");
        }

        User user = new User(0, account, Encoder.sha256(password), email);
        userService.register(user);
        return ResponseEntity.ok("register successfully");
    }

    // 3. 重置密码

    @GetMapping("/send_email")
    public ResponseEntity<APIResponse> send_email(@RequestParam String email) {
        User res_user = userService.check_user_by_email(email);
        if (!isValidEmail(email)){
            return ResponseEntity.ok(new APIResponse("邮箱格式不正确，请重新输入", 200));
        } else {
            String code = EmailSender.email_sender(email);
            String jwt_value = JwtUtil.createJWT("email", email, code);
            System.out.println(jwt_value);
            APIResponse response = new APIResponse("send successfully", 200, jwt_value);
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/reset_password")
    public ResponseEntity<String> reset_password(@RequestParam String account, @RequestParam String new_password, @RequestParam String email, @RequestParam String code, @RequestParam String jwt_value) {
        User res_user = userService.find_user_by_account(account);
        try {
            if (!JwtUtil.paraJWT2code(jwt_value).equals(Encoder.sha256(code))) {
                return ResponseEntity.ok("验证码错误");
            }
        } catch (Exception e) {
            return ResponseEntity.ok("验证码已失效，请重新发送");
        }
        if (res_user == null)
            return ResponseEntity.ok("用户名不存在");
        else if (!res_user.getEmail().equals(email))
            return ResponseEntity.ok("邮箱与帐户名不匹配");
        else if (new_password.length() < 6)
            return ResponseEntity.ok("新密码过短");
        else {
            userService.update_password_by_account(account, Encoder.sha256(new_password));
            return ResponseEntity.ok("reset successfully");
        }
    }

    public boolean isValidEmail(String email) {
        String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }
}