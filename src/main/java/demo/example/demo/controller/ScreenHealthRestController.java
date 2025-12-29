package demo.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.example.demo.dto.ScreenHealthDto;
import demo.example.demo.service.ScreenHealthService;

@RestController
@RequestMapping("/screens-health")
public class ScreenHealthRestController {

    private final ScreenHealthService screenHealthService;

    public ScreenHealthRestController(ScreenHealthService screenHealthService) {
        this.screenHealthService = screenHealthService;
    }

    @GetMapping
    public List<ScreenHealthDto> getHealth() {
        return screenHealthService.getScreenHealth();
    }
}
