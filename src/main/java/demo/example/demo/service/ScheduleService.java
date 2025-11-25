package demo.example.demo.service;

import demo.example.demo.entity.Schedule;
import demo.example.demo.repositories.ScheduleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleService {

    private final ScheduleRepository repo;

    public ScheduleService(ScheduleRepository repo) {
        this.repo = repo;
    }

    public List<Schedule> getForScreen(Integer screenId) {
        return repo.findByScreen_Id(screenId);
    }
}
