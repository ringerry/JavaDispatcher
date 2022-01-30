package Ru.IVT.JWT_REST_Dispatcher.BrowserPages;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
//@RequestMapping(value = "/greeting")
public class GetPages {


    @GetMapping("/greeting")
    public String greeting(@RequestParam(name="name", required=false, defaultValue="Так вот") String name, Model model) {
        model.addAttribute("name", name);
        return "greeting";
    }

}